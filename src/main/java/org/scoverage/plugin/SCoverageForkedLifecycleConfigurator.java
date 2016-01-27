/*
 * Copyright 2014-2016 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

/**
 * Configures project and dependent modules in multi-module project when entering forked {@code scoverage}
 * life cycle and restores original configuration after leaving it.
 * <br>
 * In default life cycle modules use:
 * <ul>
 * <li>
 * {@code ${project.build.directory}/classes} as {@code project.outputDirectory} property value,
 * </li>
 * <li>
 * {@code ${project.build.directory}/${project.finalName}.jar} as {@code project.artifact.file} property value.
 * </li>
 * </ul>
 * <br>
 * In forked {@code scoverage} life cycle modules use:
 * <ul>
 * <li>
 * {@code ${project.build.directory}/scoverage-classes} as {@code project.outputDirectory} property value,
 * </li>
 * <li>
 * {@code ${project.build.directory}/scoverage-${project.finalName}.jar} as {@code project.artifact.file} property value.
 * </li>
 * </ul>
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public class SCoverageForkedLifecycleConfigurator
{
    private final static String PROP_ORIG_OUTPUT_DIRECTORY = "_scoverage.original.outputDirectory";
    private final static String PROP_ORIG_ARTIFACT_FILE = "_scoverage.original.artifactFile";

    private final static String PROP_FORKED_OUTPUT_DIRECTORY = "_scoverage.forked.outputDirectory";
    private final static String PROP_FORKED_ARTIFACT_FILE = "_scoverage.forked.artifactFile";

    /**
     * Configures project and dependent modules in multi-module project when entering forked {@code scoverage}
     * life cycle.
     * 
     * @param project Maven project in {@code scoverage} forked life cycle.
     * @param reactorProjects all reactor Maven projects.
     */
    public static void afterForkedLifecycleEnter( MavenProject project, List<MavenProject> reactorProjects )
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

    /**
     * Restores original configuration after leaving forked {@code scoverage} life cycle.
     * <br>
     * {@code project} is a project in default life cycle, {@code project.getExecutionProject()}
     * is a project in just finished forked {@code scoverage} life cycle.
     * 
     * @param project Maven project in default life cycle.
     * @param reactorProjects all reactor Maven projects.
     */
    public static void afterForkedLifecycleExit( MavenProject project, List<MavenProject> reactorProjects )
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