/*
 * Copyright 2014-2026 Grzegorz Slowikowski (gslowikowski at gmail dot com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.scoverage.plugin;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import scala.Tuple2;
import scala.jdk.javaapi.CollectionConverters;

import scoverage.domain.Coverage;
import scoverage.domain.CoverageMetrics;
import scoverage.domain.DoubleFormat;
import scoverage.domain.MeasuredFile;
import scoverage.domain.MeasuredPackage;
import scoverage.reporter.IOUtils;
import scoverage.serialize.Serializer;

/**
 * Checks if minimum code coverage by unit tests reached
 * in forked {@code scoverage} life cycle.
 * <br>
 * <br>
 * In forked {@code scoverage} life cycle project is compiled with SCoverage instrumentation
 * and unit tests are executed before checking.
 * <br>
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "check", threadSafe = true )
@Execute( lifecycle = "scoverage", phase = LifecyclePhase.TEST )
public class SCoverageCheckMojo
    extends AbstractMojo
{

    /**
     * Allows SCoverage to be skipped.
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * Directory where the coverage files should be written.
     */
    @Parameter( property = "scoverage.dataDirectory", defaultValue = "${project.build.directory}/scoverage-data", required = true, readonly = true )
    private File dataDirectory;

    /**
     * Required minimum total statement coverage.
     * <br>
     * <br>
     * See <a href="https://github.com/scoverage/sbt-scoverage#minimum-coverage">https://github.com/scoverage/sbt-scoverage#minimum-coverage</a> for additional documentation.
     * <br>
     *
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.minimumCoverage", defaultValue = "0" )
    private Double minimumCoverage;

    /**
     * Required minimum total branch coverage.
     * <br>
     * <br>
     * See <a href="https://github.com/scoverage/sbt-scoverage#minimum-coverage">https://github.com/scoverage/sbt-scoverage#minimum-coverage</a> for additional documentation.
     * <br>
     *
     * @since 2.0.1
     */
    @Parameter( property = "scoverage.minimumCoverageBranchTotal", defaultValue = "0" )
    private Double minimumCoverageBranchTotal;

    /**
     * Required minimum per-package statement coverage.
     * <br>
     * <br>
     * See <a href="https://github.com/scoverage/sbt-scoverage#minimum-coverage">https://github.com/scoverage/sbt-scoverage#minimum-coverage</a> for additional documentation.
     * <br>
     *
     * @since 2.0.1
     */
    @Parameter( property = "scoverage.minimumCoverageStmtPerPackage", defaultValue = "0" )
    private Double minimumCoverageStmtPerPackage;

    /**
     * Required minimum per-package branch coverage.
     * <br>
     * <br>
     * See <a href="https://github.com/scoverage/sbt-scoverage#minimum-coverage">https://github.com/scoverage/sbt-scoverage#minimum-coverage</a> for additional documentation.
     * <br>
     *
     * @since 2.0.1
     */
    @Parameter( property = "scoverage.minimumCoverageBranchPerPackage", defaultValue = "0" )
    private Double minimumCoverageBranchPerPackage;

    /**
     * Required minimum per-file statement coverage.
     * <br>
     * <br>
     * See <a href="https://github.com/scoverage/sbt-scoverage#minimum-coverage">https://github.com/scoverage/sbt-scoverage#minimum-coverage</a> for additional documentation.
     * <br>
     *
     * @since 2.0.1
     */
    @Parameter( property = "scoverage.minimumCoverageStmtPerFile", defaultValue = "0" )
    private Double minimumCoverageStmtPerFile;

    /**
     * Required minimum per-file branch coverage.
     * <br>
     * <br>
     * See <a href="https://github.com/scoverage/sbt-scoverage#minimum-coverage">https://github.com/scoverage/sbt-scoverage#minimum-coverage</a> for additional documentation.
     * <br>
     *
     * @since 2.0.1
     */
    @Parameter( property = "scoverage.minimumCoverageBranchPerFile", defaultValue = "0" )
    private Double minimumCoverageBranchPerFile;

    /**
     * Fail the build if minimum coverage was not reached.
     * <br>
     * <br>
     * See <a href="https://github.com/scoverage/sbt-scoverage#minimum-coverage">https://github.com/scoverage/sbt-scoverage#minimum-coverage</a> for additional documentation.
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.failOnMinimumCoverage", defaultValue = "false" )
    private boolean failOnMinimumCoverage;

    /**
     * The file encoding to use when reading Scala sources.
     * <br>
     *
     * @since 1.4.12
     */
    @Parameter( property = "encoding", defaultValue = "${project.build.sourceEncoding}" )
    private String encoding;

    /**
     * Maven project to interact with.
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    /**
     * All Maven projects in the reactor.
     */
    @Parameter( defaultValue = "${reactorProjects}", required = true, readonly = true )
    private List<MavenProject> reactorProjects;

    /**
     * Checks tests coverage and optionally fails the build if minimum level not reached.
     * 
     * @throws MojoFailureException if coverage is below minimumCoverage and failOnMinimumCoverage option set
     */
    @Override
    public void execute() throws MojoFailureException
    {
        if ( "pom".equals( project.getPackaging() ) )
        {
            getLog().info( "Skipping SCoverage execution for project with packaging type 'pom'" );
            //for aggragetor mojo - list of submodules: List<MavenProject> modules = project.getCollectedProjects();
            return;
        }

        if ( skip )
        {
            getLog().info( "Skipping Scoverage execution" );
            return;
        }

        long ts = System.currentTimeMillis();

        SCoverageForkedLifecycleConfigurator.afterForkedLifecycleExit( project, reactorProjects );

        if ( !dataDirectory.exists() || !dataDirectory.isDirectory() )
        {
            getLog().info( "Cannot perform check, instrumentation not performed - skipping" );
            return;
        }

        File coverageFile = Serializer.coverageFile( dataDirectory );
        if ( !coverageFile.exists() )
        {
            getLog().info( "Scoverage data file does not exist. Skipping check" );
            return;
        }
        if ( !coverageFile.isFile() )
        {
            getLog().info( "Scoverage data file is a directory, not a file. Skipping check" );
            return;
        }

        Coverage coverage = Serializer.deserialize( coverageFile, project.getBasedir() );
        List<File> measurementFiles = Arrays.asList( IOUtils.findMeasurementFiles( dataDirectory ) );
        scala.collection.Set<Tuple2<Object, String>> measurements =
                IOUtils.invoked( CollectionConverters.asScala( measurementFiles ).toSeq(), encoding );
        coverage.apply( measurements );

        int branchCount = coverage.branchCount();
        int statementCount = coverage.statementCount();
        int invokedBranchesCount = coverage.invokedBranchesCount();
        int invokedStatementCount = coverage.invokedStatementCount();

        getLog().debug( String.format( "invokedBranchesCount:%d / branchCount:%d, invokedStatementCount:%d / statementCount:%d",
                                      invokedBranchesCount, branchCount, invokedStatementCount, statementCount ) );

        boolean ok = checkCoverage( getLog(), "Total", coverage,
                                    minimumCoverage, minimumCoverageBranchTotal, true );
        ok = checkCoverage( getLog(), "Package:", coverage.packages(), MeasuredPackage::name,
                            minimumCoverageStmtPerPackage, minimumCoverageBranchPerPackage ) && ok;
        ok = checkCoverage( getLog(), "File:", coverage.files(), MeasuredFile::filename,
                            minimumCoverageStmtPerFile, minimumCoverageBranchPerFile ) && ok;

        if ( !ok && failOnMinimumCoverage )
        {
            throw new MojoFailureException( "Coverage minimum was not reached" );
        }

        long te = System.currentTimeMillis();
        getLog().debug( String.format( "Mojo execution time: %d ms", te - ts ) );
    }

    // Private utility methods

    private static boolean is100( Double d )
    {
        return Math.abs( 100 - d ) <= 0.00001d;
    }

    private static <T extends CoverageMetrics >
        boolean checkCoverage( Log logger, String metricPrefix,
                               scala.collection.Iterable< T > metrics,
                               Function< T, String > toName,
                               double minStmt, double minBranch )
    {
        return minStmt <= 0 && minBranch <= 0 || checkAll(metrics, cov ->
            checkCoverage(logger, metricPrefix + toName.apply(cov), cov, minStmt, minBranch, false)
        );
    }

    private static boolean checkCoverage( Log logger, String metric, CoverageMetrics metrics,
                                          double minStmt, double minBranch, boolean logSuccessInfo )
    {
        boolean stmt = checkCoverage( logger, "Statement:" + metric,
                                      minStmt, metrics.statementCoveragePercent(), logSuccessInfo );
        boolean branch = checkCoverage( logger, "Branch:" + metric,
                                        minBranch, metrics.branchCoveragePercent(), logSuccessInfo );
        return stmt && branch;
    }

    private static boolean checkCoverage( Log logger, String metric,
                                          double minimum, double actual, boolean logSuccessInfo )
    {
        if ( minimum <= 0 )
        {
            return true;
        }

        if ( is100( actual ) )
        {
            logSuccess( logger, String.format( "Coverage is 100%%: %s!", metric ), logSuccessInfo );
            return true;
        }

        String minimumFormatted = DoubleFormat.twoFractionDigits( minimum );
        String actualFormatted = DoubleFormat.twoFractionDigits( actual );
        boolean ok = minimum <= actual;

        if ( ok )
        {
            String message = String.format( "Coverage is above minimum [%s%% >= %s%%]: %s",
                                            actualFormatted, minimumFormatted, metric );
            logSuccess( logger, message, logSuccessInfo );
        }
        else
        {
            String message = String.format( "Coverage is below minimum [%s%% < %s%%]: %s",
                                            actualFormatted, minimumFormatted, metric );
            logger.error( message );
        }

        return ok;
    }

    private static void logSuccess( Log logger, String message, boolean logSuccessInfo )
    {
        if ( logSuccessInfo )
        {
            logger.info( message );
        }
        else
        {
            logger.debug( message );
        }
    }

    private static <T> boolean checkAll( scala.collection.Iterable<T> iterable, Predicate<T> predicate )
    {
        boolean ok = true;
        for ( T elem : CollectionConverters.asJava( iterable ) )
        {
            ok = predicate.test( elem ) && ok;
        }
        return ok;
    }

}
