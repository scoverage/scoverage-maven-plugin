/*
 * Copyright 2014 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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
import scoverage.Coverage;
import scoverage.IOUtils;
import scoverage.Serializer;
import scoverage.report.CoberturaXmlWriter;
import scoverage.report.ScoverageHtmlWriter;
import scoverage.report.ScoverageXmlWriter;

/**
 * Generates SCoverage report in forked {@code scoverage} life cycle.
 * <br/>
 * <br/>
 * In forked {@code scoverage} life cycle project is compiled with SCoverage instrumentation
 * and tests are executed before report generation.
 * <br/>
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
     * <br/>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * Specifies if the build will fail if there are errors during javadoc execution or not.
     */
    @Parameter( property = "scoverage.failOnError", defaultValue = "true", readonly = true )
    private boolean failOnError;

    /**
     * Maven project to interact with.
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    /**
     * Directory where the coverage files should be written.
     * <br/>
     *
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.dataDirectory", defaultValue = "${project.build.directory}/scoverage-data", required = true, readonly = true )
    private File dataDirectory;

    /**
     * Specifies the destination directory where SCoverage saves the generated HTML files.
     */
    @Parameter( property = "scoverage.outputDirectory", defaultValue = "${project.reporting.outputDirectory}/scoverage", required = true, readonly = true )
    private File outputDirectory;

    /**
     * Specifies the destination directory where SCoverage saves the generated HTML files.
     */
    @Parameter( property = "scoverage.xmlOutputDirectory", defaultValue = "${project.build.directory}", required = true, readonly = true )
    private File xmlOutputDirectory;

    /**
     * The name of the destination directory.
     * <br/>
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
     * The description of the Javadoc report to be displayed in the Maven Generated Reports page
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
        try
        {
            long ts = System.currentTimeMillis();

            File sourceDir = new File( project.getBuild().getSourceDirectory() );

            mkdirs( outputDirectory );
            mkdirs( xmlOutputDirectory );
            
            /*does not work, because empty "index.html" is already there and cannot be deleted.
            File reportOutputDir = getReportOutputDirectory();
            if ( reportOutputDir.exists() )
            {
                if ( reportOutputDir.isDirectory() )
                {
                    try
                    {
                        FileUtils.deleteDirectory( reportOutputDir );
                    }
                    catch ( IOException e)
                    {
                        throw new MavenReportException( String.format( "Cannot delete \"%s\" directory", reportsDir.getAbsolutePath() ), e ); 
                    }
                }
                else
                {
                   throw new MavenReportException( String.format( "\"%s\" is not a directory", reportsDir.getAbsolutePath() ) ); 
                }
            }*/

            File coverageFile = Serializer.coverageFile( dataDirectory );
            Coverage coverage = Serializer.deserialize( coverageFile );

            File[] measurementFiles = IOUtils.findMeasurementFiles( dataDirectory );
            scala.collection.Set<Object> measurements = IOUtils.invoked( Predef$.MODULE$
                .wrapRefArray( measurementFiles ) );
            coverage.apply( measurements );

            getLog().info( "[scoverage] Generating cobertura XML report..." );
            new CoberturaXmlWriter( project.getBasedir(), xmlOutputDirectory ).write( coverage );

            getLog().info( "[scoverage] Generating scoverage XML report..." );
            new ScoverageXmlWriter( sourceDir, xmlOutputDirectory, false ).write( coverage );

            getLog().info( "[scoverage] Generating scoverage HTML report..." );
            new ScoverageHtmlWriter( sourceDir, outputDirectory ).write( coverage );

            long te = System.currentTimeMillis();
            getLog().debug( String.format( "Mojo execution time: %d ms", te - ts ) );
        }
        catch ( RuntimeException e )
        {
            if ( failOnError )
            {
                throw e;
            }
            getLog().error( "Error while creating javadoc report: " + e.getMessage(), e );
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
            failOnError( "An error has occurred in " + getName( Locale.ENGLISH ) + " report generation", e );
        }
        catch ( RuntimeException e )
        {
            failOnError( "An error has occurred in " + getName( Locale.ENGLISH ) + " report generation", e );
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

    private void failOnError( String prefix, Exception e )
        throws MojoExecutionException
    {
        if ( failOnError )
        {
            if ( e instanceof RuntimeException )
            {
                throw (RuntimeException) e;
            }
            throw new MojoExecutionException( prefix + ": " + e.getMessage(), e );
        }

        getLog().error( prefix + ": " + e.getMessage(), e );
    }

    private void mkdirs( File directory )
        throws MavenReportException
    {
        if ( !directory.exists() && !directory.mkdirs() )
        {
            throw new MavenReportException( String.format( "Cannot create \"%s\" directory ",
                                                           directory.getAbsolutePath() ) );
        }
    }

}