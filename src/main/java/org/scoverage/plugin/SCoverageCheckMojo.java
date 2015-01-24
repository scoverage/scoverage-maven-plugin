/*
 * Copyright 2014-2015 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import scala.Predef$;

import scoverage.Coverage;
import scoverage.IOUtils;
import scoverage.Serializer;

/**
 * Checks if minimum coverage reached in forked {@code scoverage} life cycle.
 * <br/>
 * <br/>
 * In forked {@code scoverage} life cycle project is compiled with SCoverage instrumentation
 * and tests are executed before checking.
 * <br/>
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "check", threadSafe = false )
@Execute( lifecycle = "scoverage", phase = LifecyclePhase.TEST )
public class SCoverageCheckMojo
    extends AbstractMojo
{

    /**
     * Allows SCoverage to be skipped.
     * <br/>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * Directory where the coverage files should be written.
     * <br/>
     *
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.dataDirectory", defaultValue = "${project.build.directory}/scoverage-data", required = true, readonly = true )
    private File dataDirectory;

    /**
     * Required minimum coverage.
     * <br/>
     * <br/>
     * See <a href="https://github.com/scoverage/sbt-scoverage#minimum-coverage">https://github.com/scoverage/sbt-scoverage#minimum-coverage</a> for additional documentation.
     * <br/>
     *
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.minimumCoverage", defaultValue = "0" )
    private Double minimumCoverage;

    /**
     * Fail the build if minimum coverage was not reached.
     * <br/>
     * <br/>
     * See <a href="https://github.com/scoverage/sbt-scoverage#minimum-coverage">https://github.com/scoverage/sbt-scoverage#minimum-coverage</a> for additional documentation.
     * <br/>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.failOnMinimumCoverage", defaultValue = "false" )
    private boolean failOnMinimumCoverage;

    /**
     * Maven project to interact with.
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

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

        if ( !dataDirectory.exists() || !dataDirectory.isDirectory() )
        {
            getLog().info( "Cannot perform check, instrumentation not performed - skipping" );
            return;
        }

        File coverageFile = Serializer.coverageFile( dataDirectory );
        if ( !coverageFile.exists() )
        {
            getLog().info( "Scoverage data file does not exist. Skipping report generation" );
            return;
        }
        if ( !coverageFile.isFile() )
        {
            getLog().info( "Scoverage data file is a directory, not a file. Skipping report generation" );
            return;
        }

        Coverage coverage = Serializer.deserialize( coverageFile );
        File[] measurementFiles = IOUtils.findMeasurementFiles( dataDirectory );
        scala.collection.Set<Object> measurements = IOUtils.invoked( Predef$.MODULE$.wrapRefArray( measurementFiles ) );
        coverage.apply( measurements );

        int branchCount = coverage.branchCount();
        int statementCount = coverage.statementCount();
        int invokedBranchesCount = coverage.invokedBranchesCount();
        int invokedStatementCount = coverage.invokedStatementCount();
        getLog().info( String.format( "DEBUG: invokedBranchesCount:%d / branchCount:%d, invokedStatementCount:%d / statementCount:%d",
                                      invokedBranchesCount, branchCount, invokedStatementCount, statementCount ) );
        if ( minimumCoverage > 0.0 )
        {
            if ( is100( minimumCoverage ) && is100( coverage.statementCoveragePercent() ) )
            {
                getLog().info( "[scoverage] 100% Coverage !" );
            }
            else if ( minimumCoverage > coverage.statementCoveragePercent() )
            {
                getLog().error( String.format( "[scoverage] Coverage is below minimum [%s%% < %f%%]",
                                               coverage.statementCoverageFormatted(), minimumCoverage ) );
                if ( failOnMinimumCoverage )
                {
                    throw new MojoFailureException( "Coverage minimum was not reached" );
                }
            }
            else
            {
                getLog().info( String.format( "[scoverage] Coverage is above minimum [%s%% < %f%%]",
                                              coverage.statementCoverageFormatted(), minimumCoverage ) );
            }
        }
        getLog().info( String.format( "[scoverage] All done. Coverage was [%s%%]",
                                      coverage.statementCoverageFormatted() ) );

        long te = System.currentTimeMillis();
        getLog().debug( String.format( "Mojo execution time: %d ms", te - ts ) );
    }

    // Private utility methods

    private boolean is100( Double d )
    {
        return Math.abs( 100 - d ) <= 0.00001d;
    }

}