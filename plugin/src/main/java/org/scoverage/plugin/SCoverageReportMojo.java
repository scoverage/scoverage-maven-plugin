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

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

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
 * Generates instrumented classes, runs tests and creates SCoverage reports.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "report", threadSafe = false )
@Execute( lifecycle = "scoverage", phase = LifecyclePhase.TEST )
public class SCoverageReportMojo
    extends AbstractMavenReport
{
    /**
     * Directory where the coverage files should be written.
     * <br/>
     *
     * @since 1.0.0
     */
    //The same parameter is in "prepare" mojo
    @Parameter( property = "scoverage.dataDir", defaultValue = "${project.build.directory}/scoverage-data" )
    private File dataDir;

    /**
     * Specifies the destination directory where SCoverage saves the generated HTML files.
     */
    @Parameter( property = "reportOutputDirectory", defaultValue = "${project.reporting.outputDirectory}/scoverage", required = true )
    private File reportOutputDirectory;

    /**
     * The name of the destination directory.
     * <br/>
     *
     * @since 1.0.0
     */
    @Parameter( property = "destDir", defaultValue = "scoverage" )
    private String destDir;

    /**
     * The name of the SCoverage report to be displayed in the Maven Generated Reports page
     * (i.e. <code>project-reports.html</code>).
     *
     * @since 1.0.0
     */
    @Parameter( property = "name" )
    private String name;

    /**
     * The description of the Javadoc report to be displayed in the Maven Generated Reports page
     * (i.e. <code>project-reports.html</code>).
     *
     * @since 1.0.0
     */
    @Parameter( property = "description" )
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
        if ( "pom".equals( project.getPackaging() ) )
        {
            return false;
        }
//        return !"pom".equals( project.getPackaging() );
        //return true;//TODO-don't generate for aggregator nodes 

        File coverageFile = Serializer.coverageFile( dataDir );
        if ( !coverageFile.exists() || !coverageFile.isFile() )
        {
//            getLog().warn( "[scoverage] No coverage data, report generation skipped." );
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public File getReportOutputDirectory()
    {
        if ( reportOutputDirectory == null )
        {
            return outputDirectory;
        }

        return reportOutputDirectory;
    }

    /**
     * Method to set the directory where the generated reports will be put
     *
     * @param reportOutputDirectory the directory file to be set
     */
    @Override
    public void setReportOutputDirectory( File reportOutputDirectory )
    {
        updateReportOutputDirectory( reportOutputDirectory, destDir );
    }

    /*?public void setDestDir( String destDir )
    {
        this.destDir = destDir;
        updateReportOutputDirectory( reportOutputDirectory, destDir );
    }*/

    private void updateReportOutputDirectory( File reportOutputDirectory, String destDir )
    {
        if ( reportOutputDirectory != null && destDir != null
             && !reportOutputDirectory.getAbsolutePath().endsWith( destDir ) )
        {
            this.reportOutputDirectory = new File( reportOutputDirectory, destDir );
        }
        else
        {
            this.reportOutputDirectory = reportOutputDirectory;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        long ts = System.currentTimeMillis();

        File targetDir = new File( project.getBuild().getDirectory() );
//        File coberturaDir = new File( targetDir, "coverage-report" );
//        File reportsDir = new File( targetDir, "scoverage-report" );
        File dataDir = new File( targetDir, "scoverage-data" );
        File sourceDir = new File( project.getBuild().getSourceDirectory() );

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

        File coverageFile = Serializer.coverageFile( dataDir );
        Coverage coverage = Serializer.deserialize( coverageFile );

        File[] measurementFiles = IOUtils.findMeasurementFiles( dataDir );
        scala.collection.Set<Object> measurements = IOUtils.invoked( Predef$.MODULE$.wrapRefArray( measurementFiles ) );
        coverage.apply( measurements );

        getLog().info( "[scoverage] Generating cobertura XML report..." );
        new CoberturaXmlWriter( project.getBasedir(), targetDir/*coberturaDir*/ ).write( coverage );

        getLog().info( "[scoverage] Generating scoverage XML report..." );
        new ScoverageXmlWriter( sourceDir, targetDir/*reportsDir*/, false ).write( coverage );

        getLog().info( "[scoverage] Generating scoverage HTML report..." );
        new ScoverageHtmlWriter( sourceDir, getReportOutputDirectory() ).write( coverage );

        long te = System.currentTimeMillis();
        getLog().debug( String.format( "Mojo execution time: %d ms", te - ts ) );
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

}