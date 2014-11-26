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

import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * ...
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "post-compile", defaultPhase = LifecyclePhase.PROCESS_CLASSES ) 
public class SCoveragePostCompileMojo
    extends AbstractMojo
{
    /**
     * Allows SCoverage to be skipped.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * Maven project to interact with.
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;

    /**
     * ...
     */
    @Override
    public void execute()
    {
        if ( "pom".equals( project.getPackaging() ) )
        {
            //?getLog().info( "Skipping SCoverage execution for project with packaging type 'pom'" );
            return;
        }

        if ( skip )
        {
            //?getLog().info( "Skipping Scoverage execution" );
            return;
        }
        
        long ts = System.currentTimeMillis();

        Properties projectProperties = project.getProperties();

        restoreProperty( projectProperties, "sbt._scalacOptions" );
        restoreProperty( projectProperties, "sbt._scalacPlugins" );
        restoreProperty( projectProperties, "addScalacArgs" );
        restoreProperty( projectProperties, "analysisCacheFile" );
        restoreProperty( projectProperties, "maven.test.failure.ignore" );

        long te = System.currentTimeMillis();
        getLog().debug( String.format( "Mojo execution time: %d ms", te - ts ) );
    }

    private void restoreProperty( Properties projectProperties, String propertyName )
    {
        if ( projectProperties.containsKey( "scoverage.backup." + propertyName ) )
        {
            String oldValue = projectProperties.getProperty( "scoverage.backup." + propertyName );
            projectProperties.put( propertyName, oldValue );
            projectProperties.remove( "scoverage.backup." + propertyName );
        }
        else
        {
            projectProperties.remove( propertyName );
        }
    }

}