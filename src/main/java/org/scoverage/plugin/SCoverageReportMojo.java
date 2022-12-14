/*
 * Copyright 2014-2022 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.doxia.module.xhtml.decoration.render.RenderingContext;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;

import org.codehaus.plexus.util.StringUtils;

import scala.Option;
import scala.Tuple2;
import scala.collection.immutable.Seq;
import scala.jdk.javaapi.CollectionConverters;

import scoverage.domain.Constants;
import scoverage.domain.Coverage;
import scoverage.domain.Statement;
import scoverage.reporter.IOUtils;
import scoverage.serialize.Serializer;
import scoverage.reporter.CoberturaXmlWriter;
import scoverage.reporter.CoverageAggregator;
import scoverage.reporter.ScoverageHtmlWriter;
import scoverage.reporter.ScoverageXmlWriter;

/**
 * Generates code coverage by unit tests report in forked {@code scoverage} life cycle.
 * <br>
 * <br>
 * In forked {@code scoverage} life cycle project is compiled with SCoverage instrumentation
 * and unit tests are executed before report generation.
 * <br>
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "report", threadSafe = false )
@Execute( lifecycle = "scoverage", phase = LifecyclePhase.TEST )
public class SCoverageReportMojo
    extends AbstractMojo
    implements MavenReport
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
     * In multi-module project additionally generate aggregated SCoverage report.
     * <br>
     * 
     * @since 1.1.0
     */
    @Parameter( property = "scoverage.aggregate", defaultValue = "false" )
    private boolean aggregate;

    /**
     * In multi-module project generate only aggregated SCoverage report.
     * <br>
     * <br>
     * Scoverage reports for individual modules will not be generated.
     * <br>
     *
     * @since 1.4.0
     */
    @Parameter( property = "scoverage.aggregateOnly", defaultValue = "false" )
    private boolean aggregateOnly;

    /**
     * The file encoding to use when reading Scala sources.
     * <br>
     * 
     * @since 1.2.0
     */
    @Parameter( property = "encoding", defaultValue = "${project.build.sourceEncoding}" )
    private String encoding;

    /**
     * Specifies if the build will fail if there are errors during report execution or not.
     */
    @Parameter( property = "scoverage.failOnError", defaultValue = "true", readonly = true )
    private boolean failOnError;

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
     * Destination directory where the coverage data files should be written.
     */
    @Parameter( property = "scoverage.dataDirectory", defaultValue = "${project.build.directory}/scoverage-data", required = true, readonly = true )
    private File dataDirectory;

    /**
     * Destination directory for generated HTML report files.
     */
    @Parameter( property = "scoverage.outputDirectory", defaultValue = "${project.reporting.outputDirectory}/scoverage", required = true, readonly = true )
    private File outputDirectory;

    /**
     * Destination directory for XML report files.
     */
    @Parameter( property = "scoverage.xmlOutputDirectory", defaultValue = "${project.build.directory}", required = true, readonly = true )
    private File xmlOutputDirectory;

    /**
     * The name of the destination directory.
     * <br>
     */
    @Parameter( property = "destDir", defaultValue = "scoverage", required = true, readonly = true )
    private String destDir;

    /**
     * The name of the SCoverage report to be displayed in the Maven Generated Reports page
     * (i.e. <code>project-reports.html</code>).
     */
    @Parameter( property = "name", readonly = true )
    private String name;

    /**
     * The description of the Scoverage report to be displayed in the Maven Generated Reports page
     * (i.e. <code>project-reports.html</code>).
     */
    @Parameter( property = "description", readonly = true )
    private String description;

    /** {@inheritDoc} */
    @Override
    public String getName( Locale locale )
    {
        if ( StringUtils.isEmpty( name ) )
        {
            return getBundle( locale ).getString( "report.scoverage.name" );
        }

        return name;
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription( Locale locale )
    {
        if ( StringUtils.isEmpty( description ) )
        {
            return getBundle( locale ).getString( "report.scoverage.description" );
        }

        return description;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings( "deprecation" )
    public void generate( Sink sink, Locale locale )
        throws MavenReportException
    {
        boolean canGenerateNonAggregatedReport = canGenerateNonAggregatedReport();
        boolean canGenerateAggregatedReport = canGenerateAggregatedReport();
        boolean canAttachAggregatedReportToSite = canAttachAggregatedReportToSite();

        if ( canAttachAggregatedReportToSite && !( canGenerateNonAggregatedReport || canGenerateAggregatedReport ) )
        {
            return; // aggregated report for top level project is generated by last reactor project
        }

        try
        {
            long ts = System.currentTimeMillis();

            // If top-level project is last reactor project it should generate ONLY aggregated report here
            if ( canGenerateNonAggregatedReport )
            {
                generateReports();
            }

            // Aggregated report must be generated in last reactor project. It may be top-level
            // project (this is very rare case) or any other project.
            // Whatever project it is, it must generate report in top-level project's site directory.
            // WARNING: Last reactor project cannot have scoverage generation skipped
            // ('skip' configuration parameter set)!
            if ( canGenerateAggregatedReport )
            {
                generateAggregatedReports();
            }

            long te = System.currentTimeMillis();
            getLog().debug( String.format( "Mojo execution time: %d ms", te - ts ) );
        }
        catch ( MavenReportException e )
        {
            if ( failOnError )
            {
                throw e;
            }
            getLog().error( "Error while creating scoverage report: " + e.getMessage(), e );
        }
        catch ( RuntimeException e )
        {
            if ( failOnError )
            {
                throw new MavenReportException( "Report generation exception", e );
            }
            getLog().error( "Error while creating scoverage report: " + e.getMessage(), e );
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getOutputName()
    {
        return destDir + "/index";
    }

    /** {@inheritDoc} */
    @Override
    public boolean isExternalReport()
    {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canGenerateReport()
    {
        if ( !skip && !"pom".equals( project.getPackaging() ) )
        {
            SCoverageForkedLifecycleConfigurator.afterForkedLifecycleExit( project, reactorProjects );
        }

        boolean canGenerateNonAggregatedReport = canGenerateNonAggregatedReport();
        boolean canAttachAggregatedReportToSite = canAttachAggregatedReportToSite();

        boolean result = canGenerateNonAggregatedReport || canAttachAggregatedReportToSite; 

        if ( !result && canGenerateAggregatedReport() )
        {
            // last project, but not top-level one
            // generate here, because 'false' must be returned to Maven in order to avoid adding to this module's site
            try
            {
                generateAggregatedReports();
            }
            catch ( MavenReportException e )
            {
                throw new RuntimeException( e );
            }
        }
        return result;
    }

    private boolean canGenerateNonAggregatedReport()
    {
        if ( skip )
        {
            return false;
        }
        if ( "pom".equals( project.getPackaging() ) )
        {
            return false;
        }
        if ( aggregateOnly && reactorProjects.size() > 1 )
        {
            return false;
        }
        File coverageFile = Serializer.coverageFile( dataDirectory );
        if ( !coverageFile.exists() || !coverageFile.isFile() )
        {
            return false;
        }
        return true;
    }

    private boolean canGenerateAggregatedReport()
    {
        return ( aggregate || aggregateOnly ) && reactorProjects.size() > 1
                && project == reactorProjects.get( reactorProjects.size() - 1 );
    }

    private boolean canAttachAggregatedReportToSite()
    {
        return ( aggregate || aggregateOnly ) && reactorProjects.size() > 1 && project.isExecutionRoot();
    }

    /** {@inheritDoc} */
    @Override
    public String getCategoryName()
    {
        return MavenReport.CATEGORY_PROJECT_REPORTS;
    }

    /** {@inheritDoc} */
    @Override
    public File getReportOutputDirectory()
    {
        return outputDirectory;
    }

    /** {@inheritDoc} */
    @Override
    public void setReportOutputDirectory( File reportOutputDirectory )
    {
        updateReportOutputDirectory( reportOutputDirectory );
    }

    private void updateReportOutputDirectory( File reportOutputDirectory )
    {
        if ( reportOutputDirectory != null && destDir != null
             && !reportOutputDirectory.getAbsolutePath().endsWith( destDir ) )
        {
            this.outputDirectory = new File( reportOutputDirectory, destDir );
        }
        else
        {
            this.outputDirectory = reportOutputDirectory;
        }
    }

    /**
     * Generates SCoverage report.
     * 
     * @throws MojoExecutionException if unexpected problem occurs
     */
    @Override
    public void execute()
        throws MojoExecutionException
    {
        if ( !canGenerateReport() )
        {
            getLog().info( "Skipping SCoverage report generation" );
            return;
        }

        try
        {
            RenderingContext context = new RenderingContext( outputDirectory, getOutputName() + ".html" );
            SiteRendererSink sink = new SiteRendererSink( context );
            Locale locale = Locale.getDefault();
            generate( sink, locale );
        }
        catch ( MavenReportException e )
        {
            String prefix = "An error has occurred in " + getName( Locale.ENGLISH ) + " report generation";
            throw new MojoExecutionException( prefix + ": " + e.getMessage(), e );
        }
    }

    /**
     * Gets the resource bundle for the specified locale.
     *
     * @param locale The locale of the currently generated report.
     * @return The resource bundle for the requested locale.
     */
    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "scoverage-report", locale, getClass().getClassLoader() );
    }

    private void generateReports()
        throws MavenReportException /*, RuntimeException*/
    {
        @SuppressWarnings( "unchecked" )
        List<String> sourceDirs = project.getExecutionProject().getCompileSourceRoots();
        List<File> sourceRoots = new ArrayList<File>( sourceDirs.size() );
        for ( String dir: sourceDirs )
        {
            sourceRoots.add( new File( dir ) );
        }

        mkdirs( outputDirectory );
        mkdirs( xmlOutputDirectory );

        File coverageFile = Serializer.coverageFile( dataDirectory );
        getLog().info( String.format( "Reading scoverage instrumentation [%s]...", coverageFile.getAbsolutePath() ) );
        Coverage coverage = Serializer.deserialize( coverageFile, project.getBasedir() );

        getLog().info( String.format( "Reading scoverage measurements [%s*]...",
                                      new File( dataDirectory, Constants.MeasurementsPrefix() ).getAbsolutePath() ) );
        List<File> measurementFiles = Arrays.asList( IOUtils.findMeasurementFiles( dataDirectory ) );
        scala.collection.Set<Tuple2<Object, String>> measurements =
                IOUtils.invoked( CollectionConverters.asScala( measurementFiles ).toSeq(), encoding );
        coverage.apply( measurements );

        getLog().info( "Generating coverage reports..." );
        writeReports( coverage, sourceRoots, xmlOutputDirectory, xmlOutputDirectory, outputDirectory );
        getLog().info( "Coverage reports completed." );
    }

    private void generateAggregatedReports()
        throws MavenReportException
    {
        Coverage coverage = new Coverage();
        AtomicInteger id = new AtomicInteger();
        List<File> scoverageDataDirs = new ArrayList<File>();
        List<File> sourceRoots = new ArrayList<File>();
        MavenProject topLevelModule = null;
        for ( MavenProject module : reactorProjects )
        {
            if ( module.isExecutionRoot() )
            {
                topLevelModule = module;
            }
            else if ( !module.getPackaging().equals( "pom" ) )
            {
                File scoverageDataDir = rebase( dataDirectory, module );
                if ( scoverageDataDir.isDirectory() )
                {
                    scoverageDataDirs.add( scoverageDataDir );
                    File coverageFile = Serializer.coverageFile(scoverageDataDir);
                    if (coverageFile.exists()) {
                        Coverage subCoverage  = Serializer.deserialize(coverageFile, module.getBasedir());
                        List<File> measurementFiles = Arrays.asList( IOUtils.findMeasurementFiles( scoverageDataDir ) );
                        scala.collection.Set<Tuple2<Object, String>> measurements =
                                IOUtils.invoked( CollectionConverters.asScala( measurementFiles ).toSeq(), encoding );
                        subCoverage.apply( measurements );
                        subCoverage.statements().foreach(statement -> {
                            int statementId = id.getAndIncrement();
                            Statement copy = statement.copy(
                                    statement.location(),
                                    statementId,
                                    statement.start(),
                                    statement.end(),
                                    statement.line(),
                                    statement.desc(),
                                    statement.symbolName(),
                                    statement.treeName(),
                                    statement.branch(),
                                    statement.count(),
                                    statement.ignored(),
                                    statement.tests()
                            );
                            coverage.add(copy);
                            return null;
                        });
                    }

                    File sourceRootsFile = new File( scoverageDataDir, "source.roots" );
                    if ( sourceRootsFile.isFile() )
                    {
                        try
                        {
                            BufferedReader r = new BufferedReader( new InputStreamReader(
                                    new FileInputStream( sourceRootsFile ), "UTF-8" ) );
                            try
                            {
                                String path = r.readLine();
                                while ( path != null )
                                {
                                    sourceRoots.add( new File( path ) );
                                    path = r.readLine();
                                }
                            }
                            finally
                            {
                                r.close();
                            }
                        }
                        catch ( IOException e )
                        {
                            throw new MavenReportException( "...", e );
                        }
                    }
                }
            }
        }

        /* Empty report must be generated or top-level site will contain invalid link to non-existent Scoverage report
        if ( scoverageDataDirs.isEmpty() )
        {
            getLog().info( "No subproject data to aggregate, skipping SCoverage report generation" );
            return;
        }*/

        if ( getLog().isDebugEnabled() && scoverageDataDirs.size() > 0 )
        {
            getLog().debug( String.format( "Found %d subproject subproject scoverage data directories:",
                    scoverageDataDirs.size() ) );
            for ( File dataDir: scoverageDataDirs )
            {
                getLog().debug( String.format( "- %s", dataDir.getAbsolutePath() ) );
            }
        }
        else
        {
            getLog().info( String.format( "Found %d subproject scoverage data directories.",
                    scoverageDataDirs.size() ) );
        }

        File topLevelModuleOutputDirectory = rebase( outputDirectory, topLevelModule );
        File topLevelModuleXmlOutputDirectory = rebase( xmlOutputDirectory, topLevelModule );

        mkdirs( topLevelModuleOutputDirectory );
        mkdirs( topLevelModuleXmlOutputDirectory );

        getLog().info( "Generating coverage aggregated reports..." );
        writeReports( coverage, sourceRoots, topLevelModuleXmlOutputDirectory, topLevelModuleXmlOutputDirectory,
                      topLevelModuleOutputDirectory );
        getLog().info( "Coverage aggregated reports completed." );
    }

    private void writeReports( Coverage coverage, List<File> sourceRoots, File coberturaXmlOutputDirectory,
                               File scoverageXmlOutputDirectory, File scoverageHtmlOutputDirectory )
    {
        Seq<File> sourceRootsAsScalaSeq = CollectionConverters.asScala( sourceRoots ).toSeq();

        new CoberturaXmlWriter( sourceRootsAsScalaSeq, coberturaXmlOutputDirectory, Option.<String>apply( encoding ) ).write( coverage );
        getLog().info( String.format( "Written Cobertura XML report [%s]",
                                      new File( coberturaXmlOutputDirectory, "cobertura.xml" ).getAbsolutePath() ) );

        new ScoverageXmlWriter( sourceRootsAsScalaSeq, scoverageXmlOutputDirectory, false, Option.<String>apply( encoding ) ).write( coverage );
        getLog().info( String.format( "Written XML coverage report [%s]",
                                      new File( scoverageXmlOutputDirectory, "scoverage.xml" ).getAbsolutePath() ) );

        new ScoverageHtmlWriter( sourceRootsAsScalaSeq, scoverageHtmlOutputDirectory, Option.<String>apply( encoding ) ).write( coverage );
        getLog().info( String.format( "Written HTML coverage report [%s]",
                                      new File( scoverageHtmlOutputDirectory, "index.html" ).getAbsolutePath() ) );

        getLog().info( String.format( "Statement coverage.: %s%%", coverage.statementCoverageFormatted() ) );
        getLog().info( String.format( "Branch coverage....: %s%%", coverage.branchCoverageFormatted() ) );
    }

    private void mkdirs( File directory )
        throws MavenReportException
    {
        if ( !directory.exists() && !directory.mkdirs() )
        {
            throw new MavenReportException( String.format( "Cannot create \"%s\" directory ",
                                                           directory.getAbsolutePath() ) );
        }
        else if ( directory.exists() && !directory.isDirectory() )
        {
            throw new MavenReportException( String.format( "Directory \"%s\" exists but is not a directory ",
                                                           directory.getAbsolutePath() ) );
        }
    }

    private File rebase( File file, MavenProject otherModule )
    {
        return new File( file.getAbsolutePath().replace( project.getBasedir().getAbsolutePath(),
                                                         otherModule.getBasedir().getAbsolutePath() ) );
    }

}
