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
import java.util.List;

import org.apache.maven.project.MavenProject;

public class SCoverageForkedLifecycleConfigurator
{
    private final static String PROP_ORIG_OUTPUT_DIRECTORY = "_scoverage.original.outputDirectory";
    private final static String PROP_ORIG_ARTIFACT_FILE = "_scoverage.original.artifactFile";

    private final static String PROP_FORKED_OUTPUT_DIRECTORY = "_scoverage.forked.outputDirectory";
    private final static String PROP_FORKED_ARTIFACT_FILE = "_scoverage.forked.artifactFile";

    // project - Maven project in "scoverage" forked life cycle
    static void afterForkedLifecycleEnter( MavenProject project, List<MavenProject> reactorProjects )
    {
        File classesDirectory = new File( project.getBuild().getOutputDirectory() );
        File scoverageClassesDirectory =
            new File( classesDirectory.getParentFile(), "scoverage-" + classesDirectory.getName() );

        project.getArtifact().setFile( null );
        project.getBuild().setOutputDirectory( scoverageClassesDirectory.getAbsolutePath() );

        for ( MavenProject reactorProject : reactorProjects )
        {
            if ( reactorProject != project )
            {
                if ( reactorProject.getProperties().containsKey( PROP_FORKED_OUTPUT_DIRECTORY ) )
                {
                    String forkedOutputDirectory =
                        (String) reactorProject.getProperties().remove/* get */( PROP_FORKED_OUTPUT_DIRECTORY );
                    reactorProject.getProperties().put( PROP_ORIG_OUTPUT_DIRECTORY,
                                                        reactorProject.getBuild().getOutputDirectory() );
                    reactorProject.getBuild().setOutputDirectory( forkedOutputDirectory );
                }

                if ( reactorProject.getProperties().containsKey( PROP_FORKED_ARTIFACT_FILE ) )
                {
                    String forkedArtifactFilePath =
                        (String) reactorProject.getProperties().remove/* get */( PROP_FORKED_ARTIFACT_FILE );
                    File originalArtifactFile = reactorProject.getArtifact().getFile();
                    reactorProject.getProperties().put( PROP_ORIG_ARTIFACT_FILE,
                                                        originalArtifactFile == null ? ""
                                                                        : originalArtifactFile.getAbsolutePath() );
                    reactorProject.getArtifact().setFile( "".equals( forkedArtifactFilePath ) ? null
                                                                          : new File( forkedArtifactFilePath ) );
                }
            }
        }
    }

    // project - Maven project
    // project.getExecutionProject() - Maven project in "scoverage" forked life cycle
    static void afterForkedLifecycleExit( MavenProject project, List<MavenProject> reactorProjects )
    {
        String forkedOutputDirectory = project.getExecutionProject().getBuild().getOutputDirectory();
        project.getProperties().put( PROP_FORKED_OUTPUT_DIRECTORY, forkedOutputDirectory );

        File forkedArtifactFile = project.getExecutionProject().getArtifact().getFile();
        project.getProperties().put( PROP_FORKED_ARTIFACT_FILE,
                                     forkedArtifactFile != null ? forkedArtifactFile.getAbsolutePath() : "" );

        // Restore changed outputDirectory and artifact.file in other reactor projects
        for ( MavenProject reactorProject : reactorProjects )
        {
            if ( reactorProject != project )
            {
                if ( reactorProject.getProperties().containsKey( PROP_ORIG_OUTPUT_DIRECTORY ) )
                {
                    String originalOutputDirectory =
                        (String) reactorProject.getProperties().remove( PROP_ORIG_OUTPUT_DIRECTORY );
                    forkedOutputDirectory = reactorProject.getBuild().getOutputDirectory();

                    reactorProject.getProperties().put( PROP_FORKED_OUTPUT_DIRECTORY, forkedOutputDirectory );
                    reactorProject.getBuild().setOutputDirectory( originalOutputDirectory );
                }

                if ( reactorProject.getProperties().containsKey( PROP_ORIG_ARTIFACT_FILE ) )
                {
                    String originalArtifactFilePath =
                        (String) reactorProject.getProperties().remove/* get */( PROP_ORIG_ARTIFACT_FILE );
                    forkedArtifactFile = reactorProject.getArtifact().getFile();

                    reactorProject.getProperties().put( PROP_FORKED_ARTIFACT_FILE,
                                                        forkedArtifactFile == null ? ""
                                                                        : forkedArtifactFile.getAbsolutePath() );
                    reactorProject.getArtifact().setFile( "".equals( originalArtifactFilePath ) ? null
                                                                          : new File( originalArtifactFilePath ) );
                }
            }
        }
    }

}