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
//import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.doxia.module.xhtml.decoration.render.RenderingContext;
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

import org.codehaus.doxia.sink.Sink;

//import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import scala.Predef$;
import scala.collection.Iterator;
import scala.collection.JavaConversions;
import scala.collection.Seq;
import scala.xml.Elem;
import scala.xml.Node;
import scala.xml.NodeSeq;
import scala.xml.XML$;

import scoverage.Coverage;
import scoverage.IOUtils;
import scoverage.Serializer;
import scoverage.report.CoberturaXmlWriter;
import scoverage.report.CoverageAggregator;
import scoverage.report.ScoverageHtmlWriter;
import scoverage.report.ScoverageXmlWriter;

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
     * Aggregate SCoverage reports.
     * <br>
     * 
     * @since 1.1.0
     */
    @Parameter( property = "scoverage.aggregate", defaultValue = "false" )
    private boolean aggregate;

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
    public void generate( Sink sink, Locale locale )
        throws MavenReportException
    {
        if ( aggregate && reactorProjects.size() > 1 && project.isExecutionRoot() && project != reactorProjects.get( reactorProjects.size() - 1 ) )
        {
            return; // aggregated report for top level project is generated by last reactor project
        }

        try
        {
            long ts = System.currentTimeMillis();

            generateReports();
            
            if ( aggregate && project == reactorProjects.get( reactorProjects.size() - 1 ) )
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
        if ( skip )
        {
            return false;
        }

        if ( aggregate && reactorProjects.size() > 1 && project.isExecutionRoot() )
        {
            return true; // for aggregated report
        }

        if ( "pom".equals( project.getPackaging() ) )
        {
            return false;
        }
//        return !"pom".equals( project.getPackaging() );
        //return true;//TODO-don't generate for aggregator nodes 

        File coverageFile = Serializer.coverageFile( dataDirectory );
        if ( !coverageFile.exists() || !coverageFile.isFile() )
        {
//            getLog().warn( "[scoverage] No coverage data, report generation skipped." );
            return false;
        }
        return true;
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
        List<String> sourceDirs = project.getExecutionProject().getCompileSourceRoots();
        List<File> sourceRoots = new ArrayList<File>( sourceDirs.size() );
        for ( String dir: sourceDirs )
        {
            sourceRoots.add( new File( dir ) );
        }

        mkdirs( outputDirectory );
        mkdirs( xmlOutputDirectory );

        File coverageFile = Serializer.coverageFile( dataDirectory );
        Coverage coverage = Serializer.deserialize( coverageFile );

        File[] measurementFiles = IOUtils.findMeasurementFiles( dataDirectory );
        scala.collection.Set<Object> measurements = IOUtils.invoked( Predef$.MODULE$
            .wrapRefArray( measurementFiles ) );
        coverage.apply( measurements );

        Seq<File> sourceRootsAsScalaSeq = JavaConversions.asScalaBuffer( sourceRoots );

        getLog().info( "[scoverage] Generating cobertura XML report..." );
        new CoberturaXmlWriter( sourceRootsAsScalaSeq, xmlOutputDirectory ).write( coverage );

        getLog().info( "[scoverage] Generating scoverage XML report..." );
        new ScoverageXmlWriter( sourceRootsAsScalaSeq, xmlOutputDirectory, false ).write( coverage );

        getLog().info( "[scoverage] Generating scoverage HTML report..." );
        new ScoverageHtmlWriter( sourceRootsAsScalaSeq, outputDirectory ).write( coverage );
    }

    private void generateAggregatedReports()
        throws MavenReportException /*, RuntimeException*/
    {
        List<File> scoverageXmlFiles = new ArrayList<File>();
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
                File moduleXmlOutputDirectory = rebase( xmlOutputDirectory, module );
                File scoverageXmlFile = new File( moduleXmlOutputDirectory, "scoverage.xml" );
                if ( scoverageXmlFile.isFile() )
                {
                    scoverageXmlFiles.add( scoverageXmlFile );

                    File coberturaXmlFile = new File( moduleXmlOutputDirectory, "cobertura.xml" );
                    if ( coberturaXmlFile.isFile() )
                    {
                        Elem xml = (Elem) XML$.MODULE$.loadFile( coberturaXmlFile );
                        Node sources = xml.$bslash( "sources" ).head();
                        NodeSeq sourceSeq = sources.$bslash( "source" );
                        Iterator<Node> it = sourceSeq.iterator();
                        while ( it.hasNext() )
                        {
                            Node source = it.next();
                            String path = source.text().trim();
                            if ( !"--source".equals( path ) )
                            {
                                sourceRoots.add( new File( path ) );
                            }
                        }
                    }
                }
            }
        }

        File topLevelModuleOutputDirectory = rebase( outputDirectory, topLevelModule );
        File topLevelModuleXmlOutputDirectory = rebase( xmlOutputDirectory, topLevelModule );

        mkdirs( topLevelModuleOutputDirectory );
        mkdirs( topLevelModuleXmlOutputDirectory );

        Seq<File> sourceRootsAsScalaSeq = JavaConversions.asScalaBuffer( sourceRoots );

        Coverage coverage =
            CoverageAggregator.aggregatedCoverage( JavaConversions.asScalaBuffer( scoverageXmlFiles ).toSeq() );

        getLog().info( "[scoverage] Generating aggregated cobertura XML report..." );
        new CoberturaXmlWriter( sourceRootsAsScalaSeq, topLevelModuleXmlOutputDirectory ).write( coverage );

        getLog().info( "[scoverage] Generating aggregated scoverage XML report..." );
        new ScoverageXmlWriter( sourceRootsAsScalaSeq, topLevelModuleXmlOutputDirectory, false ).write( coverage );

        getLog().info( "[scoverage] Generating aggregated scoverage HTML report..." );
        new ScoverageHtmlWriter( sourceRootsAsScalaSeq, topLevelModuleOutputDirectory ).write( coverage );
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