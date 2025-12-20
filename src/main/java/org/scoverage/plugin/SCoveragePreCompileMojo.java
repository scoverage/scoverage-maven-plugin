/*
 * Copyright 2014-2024 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * Configures project for compilation with SCoverage instrumentation.
 * <br>
 * <br>
 * Supported compiler plugins:
 * <ul>
 * <li><a href="https://davidb.github.io/scala-maven-plugin/">net.alchim31.maven:scala-maven-plugin</a></li>
 * </ul>
 * <br>
 * This is internal mojo, executed in forked {@code scoverage} life cycle.
 * <br>
 *
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "pre-compile", defaultPhase = LifecyclePhase.GENERATE_RESOURCES )
public class SCoveragePreCompileMojo
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
     * Scala version used for scalac compiler plugin artifact resolution.
     *
     * @since 1.0.0
     */
    @Parameter( property = "scala.version" )
    private String scalaVersion;

    /**
     * Directory where the coverage files should be written.
     * <br>
     *
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.dataDirectory", defaultValue = "${project.build.directory}/scoverage-data", required = true, readonly = true )
    private File dataDirectory;

    /**
     * Semicolon-separated list of regular expressions for packages to exclude, "(empty)" for default package.
     * <br>
     * <br>
     * Example:
     * <br>
     * {@code (empty);Reverse.*;.*AuthService.*;models\.data\..*}
     * <br>
     * <br>
     * See <a href="https://github.com/scoverage/sbt-scoverage#exclude-classes-and-packages">https://github.com/scoverage/sbt-scoverage#exclude-classes-and-packages</a> for additional documentation.
     * <br>
     *
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.excludedPackages" )
    private String excludedPackages;

    /**
     * Semicolon-separated list of regular expressions for source paths to exclude.
     * <br>
     *
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.excludedFiles" )
    private String excludedFiles;

    /**
     * See <a href="https://github.com/scoverage/sbt-scoverage#highlighting">https://github.com/scoverage/sbt-scoverage#highlighting</a>.
     * <br>
     *
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.highlighting", defaultValue = "true" )
    private boolean highlighting;

    /**
     * Force <a href="https://github.com/scoverage/scalac-scoverage-plugin">scalac-scoverage-plugin</a> version used.
     * <br>
     *
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.scalacPluginVersion", defaultValue = "2.5.2" )
    private String scalacPluginVersion;

    /**
     * Semicolon-separated list of project properties set in forked {@code scoverage} life cycle.
     * <br>
     * <br>
     * Example:
     * <br>
     * {@code prop1=val1;prop2=val2;prop3=val3}
     * <br>
     *
     * @since 1.4.0
     */
    @Parameter( property = "scoverage.additionalForkedProjectProperties" )
    private String additionalForkedProjectProperties;

    /**
     * Maven project to interact with.
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    /**
     * The current Maven session.
     */
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

    /**
     * All Maven projects in the reactor.
     */
    @Parameter( defaultValue = "${reactorProjects}", required = true, readonly = true )
    private List<MavenProject> reactorProjects;

    /**
     * Repository system used to look up artifacts in the remote repository.
     */
    @Inject
    private RepositorySystem repositorySystem;

    @Parameter( defaultValue = "${repositorySystemSession}", readonly = true )
    protected RepositorySystemSession repoSession;

    @Parameter( defaultValue = "${project.remoteProjectRepositories}", readonly = true )
    protected List<RemoteRepository> remoteRepos;

    /**
     * Configures project for compilation with SCoverage instrumentation.
     *
     * @throws MojoExecutionException if unexpected problem occurs
     */
    @Override
    public void execute() throws MojoExecutionException
    {
        if ( "pom".equals( project.getPackaging() ) )
        {
            getLog().info( "Skipping SCoverage execution for project with packaging type 'pom'" );
            //for aggregator mojo - list of submodules: List<MavenProject> modules = project.getCollectedProjects();
            return;
        }

        if ( skip )
        {
            getLog().info( "Skipping Scoverage execution as configured" );

            Properties projectProperties = project.getProperties();

            // for  maven-compiler-plugin (compile), scala-maven-plugin (compile)
            setProperty( projectProperties, "maven.main.skip", "true" );

            // for maven-resources-plugin (testResources), maven-compiler-plugin (testCompile),
            // scala-maven-plugin (testCompile), maven-surefire-plugin and scalatest-maven-plugin
            setProperty( projectProperties, "maven.test.skip", "true" );
            // for scalatest-maven-plugin and specs2-maven-plugin
            setProperty( projectProperties, "skipTests", "true" );

            return;
        }

        long ts = System.currentTimeMillis();

        ScalaVersion scalaVersion = resolveScalaVersion();

        if ( scalaVersion != null )
        {
            boolean supportedScalaVersion = scalaVersion.isScala2() && scalaVersion.isAtLeast( "2.12.8" ) ||
                                            scalaVersion.isAtLeast( "3.2.0" );
            if (!supportedScalaVersion)
            {
                getLog().warn( String.format( "Skipping SCoverage execution - unsupported Scala version \"%s\". Supported Scala versions are 2.12.8+, 2.13.0+ and 3.2.0+ .",
                                              scalaVersion.full ) );
                return;
            }
        }
        else
        {
            getLog().warn( "Skipping SCoverage execution - Scala version not set" );
            return;
        }

        Map<String, String> additionalProjectPropertiesMap = null;
        if ( additionalForkedProjectProperties != null && !additionalForkedProjectProperties.isEmpty() )
        {
            String[] props = additionalForkedProjectProperties.split( ";" );
            additionalProjectPropertiesMap = new HashMap<>(props.length);
            for ( String propVal: props )
            {
                String[] tmp = propVal.split( "=", 2 );
                if ( tmp.length == 2 )
                {
                    String propName = tmp[ 0 ].trim();
                    String propValue = tmp[ 1 ].trim();
                    additionalProjectPropertiesMap.put( propName, propValue );
                }
                else
                {
                    getLog().warn( String.format( "Skipping invalid additional forked project property \"%s\", must be in \"key=value\" format",
                            propVal ) );

                }
            }
        }

        SCoverageForkedLifecycleConfigurator.afterForkedLifecycleEnter( project, reactorProjects, additionalProjectPropertiesMap );

        try
        {
            boolean scala2 = scalaVersion.isScala2();
            boolean filePackageExclusionSupportingScala3 =
                    scalaVersion.isAtLeast( "3.4.2" ) ||
                            // backported to Scala 3.3 LTS
                            ( scalaVersion.full.startsWith( "3.3." ) && scalaVersion.isAtLeast( "3.3.4" ) );

            List<Artifact> pluginArtifacts = getScoveragePluginArtifacts( scalaVersion );
            if ( scala2 ) // Scala 3 doesn't need scalac-scoverage-runtime
            {
                addScalacScoverageRuntimeDependencyToClasspath( scalaVersion );
            }

            String arg = ( scala2 ? SCALA2_DATA_DIR_OPTION : SCALA3_COVERAGE_OUT_OPTION ) + dataDirectory.getAbsolutePath();
            String addScalacArgs = arg;

            arg = scala2 ? ( SOURCE_ROOT_OPTION + session.getExecutionRootDirectory() ) : "";
            addScalacArgs = addScalacArgs + PIPE + arg;

            if ( !StringUtils.isEmpty( excludedPackages ) )
            {
                if ( scala2 ) {
                    arg = SCALA2_EXCLUDED_PACKAGES_OPTION + excludedPackages.replace( "(empty)", "<empty>" );
                    addScalacArgs = addScalacArgs + PIPE + arg;
                } else if ( filePackageExclusionSupportingScala3 ) {
                    String scala3FormatExcludedPackages = excludedPackages.replace( ";", "," );
                    arg = SCALA3_EXCLUDED_PACKAGES_OPTION + scala3FormatExcludedPackages;
                    addScalacArgs = addScalacArgs + PIPE + arg;
                } else {
                    getLog().warn( "Package exclusion is supported for Scala [3.3.4-3.4.0) or 3.4.2+" );
                }
            }

            if ( !StringUtils.isEmpty( excludedFiles ) )
            {
                if ( scala2 ) {
                    arg = SCALA2_EXCLUDED_FILES_OPTION + excludedFiles;
                    addScalacArgs = addScalacArgs + PIPE + arg;
                } else if ( filePackageExclusionSupportingScala3 ) {
                    String scala3FormatExcludedFiles = excludedFiles.replace( ";", "," );
                    arg = SCALA3_EXCLUDED_FILES_OPTION + scala3FormatExcludedFiles;
                    addScalacArgs = addScalacArgs + PIPE + arg;
                } else {
                    getLog().warn( "File exclusion is supported for Scala [3.3.4-3.4.0) or 3.4.2+" );
                }
            }

            if ( highlighting && scala2 )
            {
                addScalacArgs = addScalacArgs + PIPE + "-Yrangepos";
            }

            if ( scala2 ) {
                arg = PLUGIN_OPTION + pluginArtifacts.stream().map(x -> x.getFile().getAbsolutePath()).collect(Collectors.joining(String.valueOf(java.io.File.pathSeparatorChar)));
                addScalacArgs = addScalacArgs + PIPE + arg;
            }

            Properties projectProperties = project.getProperties();

            // for scala-maven-plugin (version 3.0.0+)
            setProperty( projectProperties, "addScalacArgs", addScalacArgs );
            // for scala-maven-plugin (version 3.1.0+)
            setProperty( projectProperties, "analysisCacheFile",
                         "${project.build.directory}/scoverage-analysis/compile" );
            // for maven-surefire-plugin and scalatest-maven-plugin
            setProperty( projectProperties, "maven.test.failure.ignore", "true" );

            // for maven-jar-plugin
            // VERY IMPORTANT! Prevents from overwriting regular project artifact file
            // with instrumented one during "integration-check" or "integration-report" execution.
            project.getBuild().setFinalName( "scoverage-" + project.getBuild().getFinalName() );

            saveSourceRootsToFile();
        }
        catch ( ArtifactResolutionException | IOException e )
        {
            throw new MojoExecutionException( "SCoverage preparation failed", e );
        }

        long te = System.currentTimeMillis();
        getLog().debug( String.format( "Mojo execution time: %d ms", te - ts ) );
    }

    // Private utility methods

    private static final String SCALA_LIBRARY_GROUP_ID = "org.scala-lang";
    private static final String SCALA2_LIBRARY_ARTIFACT_ID = "scala-library";
    private static final String SCALA3_LIBRARY_ARTIFACT_ID = "scala3-library_3";

    private static final String SCALA2_DATA_DIR_OPTION = "-P:scoverage:dataDir:";
    private static final String SCALA3_COVERAGE_OUT_OPTION = "-coverage-out:";
    private static final String SOURCE_ROOT_OPTION = "-P:scoverage:sourceRoot:";
    private static final String SCALA2_EXCLUDED_PACKAGES_OPTION = "-P:scoverage:excludedPackages:";
    private static final String SCALA3_EXCLUDED_PACKAGES_OPTION = "-coverage-exclude-classlikes:";
    private static final String SCALA2_EXCLUDED_FILES_OPTION = "-P:scoverage:excludedFiles:";
    private static final String SCALA3_EXCLUDED_FILES_OPTION = "-coverage-exclude-files:";
    private static final String PLUGIN_OPTION = "-Xplugin:";

    private static final char PIPE = '|';

    private ScalaVersion resolveScalaVersion()
    {
        String result = scalaVersion;
        if ( result == null || result.isEmpty() )
        {
            // check project direct dependencies (transitive dependencies cannot be checked in this Maven lifecycle phase)
            List<Dependency> dependencies = project.getDependencies();
            for ( Dependency dependency: dependencies )
            {
                if ( SCALA_LIBRARY_GROUP_ID.equals( dependency.getGroupId() )
                    && (
                            SCALA2_LIBRARY_ARTIFACT_ID.equals( dependency.getArtifactId() ) ||
                            SCALA3_LIBRARY_ARTIFACT_ID.equals( dependency.getArtifactId() )
                       )
                )
                {
                    result = dependency.getVersion();
                    break;
                }
            }
        }
        return result != null ? new ScalaVersion(result) : null;
    }

    private void setProperty( Properties projectProperties, String propertyName, String newValue )
    {
        if ( projectProperties.containsKey( propertyName ) )
        {
            String oldValue = projectProperties.getProperty( propertyName );
            projectProperties.put( "scoverage.backup." + propertyName, oldValue );
        }
        else
        {
            projectProperties.remove( "scoverage.backup." + propertyName );
        }

        if ( newValue != null )
        {
            projectProperties.put( propertyName, newValue );
        }
        else
        {
            projectProperties.remove( propertyName );
        }
    }

    private String getScalacPluginVersion() {
        if ( StringUtils.isEmpty(scalacPluginVersion) ) {
            throw new IllegalStateException("scalacPluginVersion is unset.");
        } else if ( scalacPluginVersion.startsWith("1.") ) {
            throw new IllegalStateException( String.format( "Unsupported scalacPluginVersion \"%s\". Please use scalacPluginVersion 2.0.0+ or use older version of scoverage-maven-plugin", scalacPluginVersion ) );
        } else {
            return scalacPluginVersion;
        }
    }

    private List<Artifact> getScoveragePluginArtifacts(ScalaVersion scalaVersion )
            throws ArtifactResolutionException {
        List<Artifact> resolvedArtifacts = new ArrayList<>();
        if ( scalaVersion.isScala2() ) // Scala 3 doesn't need scalac-scoverage-plugin
        {
            resolvedArtifacts.add(resolveScoverageArtifact("scalac-scoverage-plugin_" + scalaVersion.full ));
        }
        resolvedArtifacts.add(resolveScoverageArtifact("scalac-scoverage-domain_" + scalaVersion.compatible ));
        resolvedArtifacts.add(resolveScoverageArtifact("scalac-scoverage-serializer_" + scalaVersion.compatible ));
        return resolvedArtifacts;
    }

    /**
     * We need to tweak our test classpath for Scoverage.
     */
    @SuppressWarnings( "deprecation" ) // didn't find a good way to do this with Aether artifacts
    private void addScalacScoverageRuntimeDependencyToClasspath(ScalaVersion resolvedScalaVersion )
        throws ArtifactResolutionException {

        Set<org.apache.maven.artifact.Artifact> set = new LinkedHashSet<>(project.getDependencyArtifacts());
        set.add(toMavenClasspathArtifact(
                resolveScoverageArtifact("scalac-scoverage-runtime_" + resolvedScalaVersion.compatible)
        ));
        project.setDependencyArtifacts( set);
    }

    private org.apache.maven.artifact.Artifact toMavenClasspathArtifact( Artifact artifact ) {
        org.apache.maven.artifact.handler.DefaultArtifactHandler artifactHandler =
                new org.apache.maven.artifact.handler.DefaultArtifactHandler( artifact.getExtension() );
        artifactHandler.setAddedToClasspath(true);
        return new org.apache.maven.artifact.DefaultArtifact(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getVersion(),
                org.apache.maven.artifact.Artifact.SCOPE_COMPILE,
                artifact.getExtension(),
                artifact.getClassifier(),
                artifactHandler
        );
    }

    private Artifact resolveScoverageArtifact( String artifactId )
            throws ArtifactResolutionException {

        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact( new DefaultArtifact("org.scoverage", artifactId, "jar", getScalacPluginVersion()) );
        request.setRepositories(remoteRepos);

        ArtifactResult result = repositorySystem.resolveArtifact(repoSession, request);
        return result.getArtifact();
    }

    private void saveSourceRootsToFile() throws IOException
    {
        List<String> sourceRoots = project.getCompileSourceRoots();
        if ( !sourceRoots.isEmpty() )
        {
            if ( !dataDirectory.exists() && !dataDirectory.mkdirs() )
            {
                throw new IOException( String.format( "Cannot create \"%s\" directory ",
                        dataDirectory.getAbsolutePath() ) );
            }
            File sourceRootsFile = new File( dataDirectory, "source.roots" );
            try ( BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter( new FileOutputStream(sourceRootsFile), StandardCharsets.UTF_8 ))) {
                for ( String sourceRoot : sourceRoots ) {
                    writer.write( sourceRoot );
                    writer.newLine();
                }
            }
        }
    }
}
