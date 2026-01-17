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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

/**
 * Manages the Maven build configuration for projects undergoing Scoverage instrumentation.
 * <p>
 * This configurator is essential for integrating Scoverage into multi-module Maven projects,
 * especially in concurrent build environments. Its primary role is to modify the project's
 * build output directories and artifact paths to facilitate Scoverage's requirements (e.g.,
 * compiling instrumented code into a separate directory) and then restore the original
 * configuration for reactor projects after Scoverage processing is complete.
 * </p>
 *
 * <h2>Workflow:</h2>
 * <p>
 * The configurator operates in two main phases:
 * </p>
 * <ol>
 *     <li>
 *         <b>Entering Forked Scoverage Lifecycle ({@code afterForkedLifecycleEnter}):</b>
 *         <ul>
 *             <li>The current project's build output directory ({@code project.build.outputDirectory})
 *                 is switched from its default (e.g., {@code target/classes}) to a Scoverage-specific
 *                 directory (e.g., {@code target/scoverage-classes}).</li>
 *             <li>The project's main artifact file ({@code project.artifact.file}) is temporarily
 *                 nulled out to force Maven to re-resolve it against the new output directory.</li>
 *             <li>All direct reactor dependencies of the current project are also configured similarly,
 *                 by backing up their original paths and switching to forked paths.</li>
 *         </ul>
 *     </li>
 *     <li>
 *         <b>Exiting Forked Scoverage Lifecycle ({@code afterForkedLifecycleExit}):</b>
 *         <ul>
 *             <li>The Scoverage-specific build output directory and artifact file paths are
 *                 retrieved from the {@code project.getExecutionProject()} and stored for reference.</li>
 *             <li>For any projects in the reactor that were previously modified (identified by the presence
 *                 of backup properties), their original build configuration is restored. Note: This
 *                 restoration is not applied to the main project that entered the lifecycle, only to
 *                 the projects found in the reactor list.</li>
 *         </ul>
 *     </li>
 * </ol>
 *
 * <h2>Thread-Safety:</h2>
 * <p>
 * To ensure correctness and prevent race conditions in multi-threaded Maven builds (e.g., using {@code -T} flag),
 * all modifications to a {@code MavenProject}'s configuration are performed under a per-project {@code ReentrantLock}.
 * This guarantees that only one thread can modify a given project's state at any time, maintaining consistency.
 * </p>
 *
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public class SCoverageForkedLifecycleConfigurator
{
    private final static String PROP_PREFIX = "_scoverage.";
    private final static String ORIGINAL = "original";
    private final static String FORKED = "forked";
    private final static String OUTPUT_DIRECTORY = "outputDirectory";
    private final static String ARTIFACT_FILE = "artifactFile";

    /**
     * Builds a property name from state and property type.
     *
     * @param state either ORIGINAL or FORKED
     * @param propertyType either OUTPUT_DIRECTORY or ARTIFACT_FILE
     * @return property name like "_scoverage.original.outputDirectory"
     */
    private static String propertyName( String state, String propertyType )
    {
        return PROP_PREFIX + state + "." + propertyType;
    }

    /**
     * Thread-safe map to store locks for each Maven project.
     * Key is the project's unique identifier (groupId:artifactId:version).
     */
    private static final ConcurrentHashMap<String, ReentrantLock> projectLocks = new ConcurrentHashMap<>();

    /**
     * Executes the given action while holding the lock for the specified project.
     * Loan pattern for automatic lock management.
     *
     * @param project the Maven project to lock
     * @param action the action to execute while holding the lock
     */
    private static void withProjectLock( MavenProject project, Runnable action )
    {
        String projectId = project.getId(); // groupId:artifactId:version
        ReentrantLock lock = projectLocks.computeIfAbsent( projectId, id -> new ReentrantLock() );
        lock.lock();
        try
        {
            action.run();
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Swaps reactor project configuration between two states.
     * Thread-safe method that locks the reactor project during modification.
     *
     * @param reactorProject the reactor project to update
     * @param applyState the state (ORIGINAL or FORKED) from which to retrieve property values to apply to the project's build configuration.
     * @param backupState the state (ORIGINAL or FORKED) to which the project's *current* build configuration values will be saved.
     */
    private static void swapReactorProjectConfiguration( MavenProject reactorProject,
                                                         String applyState,
                                                         String backupState )
    {
        withProjectLock( reactorProject, () ->
        {
            String applyOutputDirProp = propertyName( applyState, OUTPUT_DIRECTORY );
            String backupOutputDirProp = propertyName( backupState, OUTPUT_DIRECTORY );
            String applyArtifactFileProp = propertyName( applyState, ARTIFACT_FILE );
            String backupArtifactFileProp = propertyName( backupState, ARTIFACT_FILE );

            if ( reactorProject.getProperties().containsKey( applyOutputDirProp ) )
            {
                String applyOutputDirectory =
                        (String) reactorProject.getProperties().get( applyOutputDirProp );
                String currentOutputDirectory = reactorProject.getBuild().getOutputDirectory();

                // Only save to target if it doesn't already exist (prevents corruption)
                if ( currentOutputDirectory != null
                        && !reactorProject.getProperties().containsKey( backupOutputDirProp ) )
                {
                    reactorProject.getProperties().put( backupOutputDirProp, currentOutputDirectory );
                }

                // Always set to source (idempotent, ensures correct state)
                reactorProject.getBuild().setOutputDirectory( applyOutputDirectory );
            }

            if ( reactorProject.getProperties().containsKey( applyArtifactFileProp ) )
            {
                String applyArtifactFilePath =
                        (String) reactorProject.getProperties().get( applyArtifactFileProp );
                Artifact currentArtifact = reactorProject.getArtifact();

                // Only save to target if it doesn't already exist (prevents corruption)
                if ( currentArtifact != null
                        && !reactorProject.getProperties().containsKey( backupArtifactFileProp ) )
                {
                    File currentArtifactFile = currentArtifact.getFile();
                    reactorProject.getProperties().put( backupArtifactFileProp,
                            currentArtifactFile == null ? ""
                                    : currentArtifactFile.getAbsolutePath() );
                }

                // Always set to source (idempotent, ensures correct state)
                reactorProject.getArtifact().setFile( "".equals( applyArtifactFilePath ) ? null
                        : new File( applyArtifactFilePath ) );
            }
        } );
    }

    /**
     * Extracts project IDs of all reactor dependencies for the given project.
     *
     * @param project The Maven project to analyze
     * @return Set of project IDs (groupId:artifactId:version) that are reactor dependencies
     */
    private static Set<String> getDependencyProjectIds( MavenProject project )
    {
        Set<String> dependencyIds = new HashSet<>();

        if ( project.getDependencyArtifacts() != null )
        {
            for ( Artifact artifact : project.getDependencyArtifacts() )
            {
                String projectId = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
                dependencyIds.add( projectId );
            }
        }

        return dependencyIds;
    }

    /**
     * Configures project and dependent modules in multi-module project when entering forked {@code scoverage}
     * life cycle.
     * <br>
     * Thread-safe for concurrent execution in multi-threaded Maven builds.
     * This method first configures the current project for scoverage,
     * then identifies and updates its reactor dependencies to also use the scoverage configuration.
     *
     * @param project Maven project in {@code scoverage} forked life cycle.
     * @param reactorProjects all reactor Maven projects.
     * @param additionalProjectPropertiesMap additional project properties to set.
     */
    public static void afterForkedLifecycleEnter( MavenProject project, List<MavenProject> reactorProjects,
                                                  Map<String, String> additionalProjectPropertiesMap )
    {
        withProjectLock( project, () ->
        {
            File classesDirectory = new File( project.getBuild().getOutputDirectory() );
            File scoverageClassesDirectory =
                    new File( classesDirectory.getParentFile(), "scoverage-" + classesDirectory.getName() );

            // Setting artifact file to null forces Maven to re-resolve it against the new output directory
            project.getArtifact().setFile( null );
            project.getBuild().setOutputDirectory( scoverageClassesDirectory.getAbsolutePath() );

            if ( additionalProjectPropertiesMap != null )
            {
                for ( Map.Entry<String, String> entry : additionalProjectPropertiesMap.entrySet() )
                {
                    project.getProperties().put( entry.getKey(), entry.getValue() );
                }
            }
        } );

        // Update reactor projects configuration, but only those that are dependencies of the current project
        Set<String> dependencyProjectIds = getDependencyProjectIds( project );
        for ( MavenProject reactorProject : reactorProjects )
        {
            if ( dependencyProjectIds.contains( reactorProject.getId() ) )
            {
                // Switch dependency to 'FORKED' (scoverage) configuration, saving its current ('ORIGINAL') values
                swapReactorProjectConfiguration(reactorProject, FORKED, ORIGINAL );
            }
        }
    }

    /**
     * Restores original configuration after leaving forked {@code scoverage} life cycle.
     * <br>
     * {@code project} is a project in default life cycle, {@code project.getExecutionProject()}
     * is a project in just finished forked {@code scoverage} life cycle.
     * <br>
     * Thread-safe for concurrent execution in multi-threaded Maven builds.
     *
     * @param project Maven project in default life cycle.
     * @param reactorProjects all reactor Maven projects.
     */
    public static void afterForkedLifecycleExit( MavenProject project, List<MavenProject> reactorProjects )
    {
        withProjectLock( project, () ->
        {
            String forkedOutputDirectory = project.getExecutionProject().getBuild().getOutputDirectory();
            File forkedArtifactFile = project.getExecutionProject().getArtifact().getFile();

            project.getProperties().put( propertyName( FORKED, OUTPUT_DIRECTORY ), forkedOutputDirectory );
            project.getProperties().put( propertyName( FORKED, ARTIFACT_FILE ),
                    forkedArtifactFile != null ? forkedArtifactFile.getAbsolutePath() : "" );
        } );

        // Restore changed outputDirectory and artifact.file in other reactor projects
        // Only check projects that might have been modified (have backup properties)
        for ( MavenProject reactorProject : reactorProjects )
        {
            if ( reactorProject.getProperties().containsKey( propertyName( ORIGINAL, OUTPUT_DIRECTORY ) )
                    || reactorProject.getProperties().containsKey( propertyName( ORIGINAL, ARTIFACT_FILE ) ) )
            {
                // Restore project to its 'ORIGINAL' configuration, saving its current ('FORKED') values
                swapReactorProjectConfiguration(reactorProject, ORIGINAL, FORKED );
            }
        }
    }

}