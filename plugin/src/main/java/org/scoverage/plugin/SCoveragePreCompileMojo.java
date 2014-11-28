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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 * Configures project for compilation with SCoverage instrumentation.
 * <br/>
 * <br/>
 * Supported compiler plugins:
 * <ul>
 * <li><a href="https://davidb.github.io/scala-maven-plugin/">net.alchim31.maven:scala-maven-plugin</a></li>
 * <li><a href="https://code.google.com/p/sbt-compiler-maven-plugin/">com.google.code.sbt-compiler-maven-plugin:sbt-compiler-maven-plugin</a></li>
 * </ul>
 * <br/>
 * This is internal mojo, executed in forked {@code cobertura} life cycle.
 * <br/>
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
     * <br/>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * Scala version used for compiler plugin artifact resolution.
     * <ul>
     * <li>if specified, and starts with {@code 2.10.} - <b>{@code scalac-scoverage-plugin_2.10}</b> will be used</li>
     * <li>if specified, and starts with {@code 2.11.} - <b>{@code scalac-scoverage-plugin_2.11}</b> will be used</li>
     * <li>if specified, but does not start with {@code 2.10.} or with {@code 2.11.} or is not specified - plugin execution will be skipped</li>
     * </ul>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "scala.version" )
    private String scalaVersion;

    /**
     * Directory where the coverage files should be written.
     * <br/>
     *
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.dataDirectory", defaultValue = "${project.build.directory}/scoverage-data", required = true, readonly = true )
    private File dataDirectory;

    /**
     * Semicolon-separated list of regular expressions for packages to exclude, "(empty)" for default package.
     * <br/>
     * <br/>
     * Example:
     * {@code (empty);Reverse.*;.*AuthService.*;models\.data\..*}
     * <br/>
     * <br/>
     * See <a href="https://github.com/scoverage/sbt-scoverage#exclude-classes-and-packages">https://github.com/scoverage/sbt-scoverage#exclude-classes-and-packages</a> for additional documentation.
     * <br/>
     *
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.excludedPackages", defaultValue = "" )
    private String excludedPackages;

    /**
     * Semicolon separated list of regular expressions for source paths to exclude.
     * <br/>
     *
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.excludedFiles", defaultValue = "" )
    private String excludedFiles;

    /**
     * See <a href="https://github.com/scoverage/sbt-scoverage#highlighting">https://github.com/scoverage/sbt-scoverage#highlighting</a>.
     * <br/>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.highlighting", defaultValue = "false" )
    private boolean highlighting;

    /**
     * Force <a href="https://github.com/scoverage/scalac-scoverage-plugin">scalac-scoverage-plugin</a> version used.
     * <br/>
     *
     * @since 1.0.0
     */
    @Parameter( property = "scoverage.scalacPluginVersion", defaultValue = "" )
    private String scalacPluginVersion;

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
     * Artifact factory used to look up artifacts in the remote repository.
     */
    @Component
    private ArtifactFactory factory;

    /**
     * Artifact resolver used to resolve artifacts.
     */
    @Component
    private ArtifactResolver resolver;

    /**
     * Location of the local repository.
     */
    @Parameter( property = "localRepository", readonly = true, required = true )
    private ArtifactRepository localRepo;

    /**
     * Remote repositories used by the resolver
     */
    @Parameter( property = "project.remoteArtifactRepositories", readonly = true, required = true )
    private List<ArtifactRepository> remoteRepos;

    /**
     * List of artifacts this plugin depends on.
     */
    @Parameter( property = "plugin.artifacts", readonly = true, required = true )
    private List<Artifact> pluginArtifacts;

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
            //for aggragetor mojo - list of submodules: List<MavenProject> modules = project.getCollectedProjects();
            return;
        }

        if ( skip )
        {
            getLog().info( "Skipping Scoverage execution" );
            return;
        }
        
        long ts = System.currentTimeMillis();

        String scalaMainVersion = null;
        if ( scalaVersion != null )
        {
            if ( scalaVersion.startsWith( "2.10." ) )
            {
                scalaMainVersion = "2.10";
            }
            else if ( scalaVersion.startsWith( "2.11." ) )
            {
                scalaMainVersion = "2.11";
            }
            else
            {
                getLog().warn( String.format( "Skipping SCoverage execution - unknown Scala version \"%s\"",
                                              scalaVersion ) );
                return;
            }
        }
        else
        {
            getLog().info( "Skipping SCoverage execution - Scala version not set" );
            return;
        }

        File classesDirectory = new File( project.getBuild().getOutputDirectory() );
        File scoverageClassesDirectory = new File( classesDirectory.getParentFile(), "scoverage-classes" );
        project.getBuild().setOutputDirectory( scoverageClassesDirectory.getAbsolutePath() );
        for ( MavenProject reactorProject : reactorProjects )
        {
            if ( reactorProject != project ) // TODO - how to include only dependent reactor projects?
            {
                classesDirectory = new File( reactorProject.getBuild().getOutputDirectory() );
                if ( !"scoverage-classes".equals( classesDirectory.getName() ) )
                {
                    scoverageClassesDirectory = new File( classesDirectory.getParentFile(), "scoverage-classes" );
                    if ( scoverageClassesDirectory.isDirectory() )
                    {
                        reactorProject.getBuild().setOutputDirectory( scoverageClassesDirectory.getAbsolutePath() );
                    }
                    //else
                    //{
                        // SCoverage probably skipped for that module
                        // TODO add info message
                    //}
                }
            }
        }

        try
        {
            Artifact pluginArtifact = getScalaScoveragePluginArtifact( scalaMainVersion );
            Artifact runtimeArtifact = getScalaScoverageRuntimeArtifact( scalaMainVersion );

            if ( pluginArtifact == null )
            {
                return; // scoverage plugin will not be configured
            }

            addScoverageDependenciesToTestClasspath( runtimeArtifact );

            String arg = DATA_DIR_OPTION + dataDirectory.getAbsolutePath();
            String _scalacOptions = quoteArgument( arg );
            String addScalacArgs = arg;

            if ( !StringUtils.isEmpty( excludedPackages ) )
            {
                arg = EXCLUDED_PACKAGES_OPTION + excludedPackages.replace( "(empty)", "<empty>" );
                _scalacOptions = _scalacOptions + SPACE + quoteArgument( arg );
                addScalacArgs = addScalacArgs + PIPE + arg;
            }

            if ( !StringUtils.isEmpty( excludedFiles ) )
            {
                arg = EXCLUDED_FILES_OPTION + excludedFiles;
                _scalacOptions = _scalacOptions + SPACE + quoteArgument( arg );
                addScalacArgs = addScalacArgs + PIPE + arg;
            }

            if ( highlighting )
            {
                _scalacOptions = _scalacOptions + SPACE + "-Yrangepos";
                addScalacArgs = addScalacArgs + PIPE + "-Yrangepos";
            }

            String _scalacPlugins =
                String.format( "%s:%s:%s", pluginArtifact.getGroupId(), pluginArtifact.getArtifactId(),
                               pluginArtifact.getVersion() );

            arg = PLUGIN_OPTION + pluginArtifact.getFile().getAbsolutePath();
            addScalacArgs = addScalacArgs + PIPE + arg;

            Properties projectProperties = project.getProperties();

            // for sbt-compiler-maven-plugin (version 1.0.0-beta5+)
            setProperty( projectProperties, "sbt._scalacOptions", _scalacOptions );
            // for sbt-compiler-maven-plugin (version 1.0.0-beta5+)
            setProperty( projectProperties, "sbt._scalacPlugins", _scalacPlugins );
            // for scala-maven-plugin (version 3.0.0+)
            setProperty( projectProperties, "addScalacArgs", addScalacArgs );
            // for scala-maven-plugin (version 3.1.0+)
            setProperty( projectProperties, "analysisCacheFile",
                         "${project.build.directory}/scoverage-analysis/compile" );
            // for maven-surefire-plugin and scalatest-maven-plugin
            setProperty( projectProperties, "maven.test.failure.ignore", "true" );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException( "SCoverage preparation failed", e );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "SCoverage preparation failed", e );
        }

        long te = System.currentTimeMillis();
        getLog().debug( String.format( "Mojo execution time: %d ms", te - ts ) );
    }

    // Private utility methods

    private static final String DATA_DIR_OPTION = "-P:scoverage:dataDir:";
    private static final String EXCLUDED_PACKAGES_OPTION = "-P:scoverage:excludedPackages:";
    private static final String EXCLUDED_FILES_OPTION = "-P:scoverage:excludedFiles:";
    private static final String PLUGIN_OPTION = "-Xplugin:";

    private static final char DOUBLE_QUOTE = '\"';
    private static final char SPACE = ' ';
    private static final char PIPE = '|';

    private String quoteArgument( String arg )
    {
        return arg.indexOf( SPACE ) >= 0 ? DOUBLE_QUOTE + arg + DOUBLE_QUOTE : arg;
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

    private Artifact getScalaScoveragePluginArtifact( String scalaMainVersion )
        throws ArtifactNotFoundException, ArtifactResolutionException
    {
        Artifact result = null;

        String resolvedScalacPluginVersion = scalacPluginVersion;
        if ( resolvedScalacPluginVersion == null || "".equals( resolvedScalacPluginVersion ) )
        {
            for ( Artifact artifact : pluginArtifacts )
            {
                if ( "org.scoverage".equals( artifact.getGroupId() )
                    && "scalac-scoverage-plugin_2.10".equals( artifact.getArtifactId() ) )
                {
                    if ( "2.10".equals( scalaMainVersion ) )
                    {
                        return artifact; // shortcut, use the same artifact plugin uses
                    }
                    resolvedScalacPluginVersion = artifact.getVersion();
                    break;
                }
            }
        }

        result =
            getResolvedArtifact( "org.scoverage", "scalac-scoverage-plugin_" + scalaMainVersion,
                                 resolvedScalacPluginVersion );
        return result;
    }

    private Artifact getScalaScoverageRuntimeArtifact( String scalaMainVersion )
        throws ArtifactNotFoundException, ArtifactResolutionException
    {
        Artifact result = null;

        String resolvedScalacRuntimeVersion = scalacPluginVersion;
        if ( resolvedScalacRuntimeVersion == null || "".equals( resolvedScalacRuntimeVersion ) )
        {
            for ( Artifact artifact : pluginArtifacts )
            {
                if ( "org.scoverage".equals( artifact.getGroupId() )
                    && "scalac-scoverage-plugin_2.10".equals( artifact.getArtifactId() ) )
                {
                    resolvedScalacRuntimeVersion = artifact.getVersion();
                    break;
                }
            }
        }

        result =
            getResolvedArtifact( "org.scoverage", "scalac-scoverage-runtime_" + scalaMainVersion,
                                 resolvedScalacRuntimeVersion );
        return result;
    }

    /**
     * We need to tweak our test classpath for Scoverage.
     *
     * @throws MojoExecutionException
     */
    private void addScoverageDependenciesToTestClasspath( Artifact scalaScoveragePluginArtifact )
        throws MojoExecutionException
    {
        Artifact providedArtifact = artifactScopeToProvided( scalaScoveragePluginArtifact );

        //if ( project.getDependencyArtifacts() != null )//TODO - remove "if"?
        //{
            Set<Artifact> set = new LinkedHashSet<Artifact>( project.getDependencyArtifacts() );
            set.add( providedArtifact );
            project.setDependencyArtifacts( set );
        //}
    }

    /**
     * Use provided instead of just test, so it's available on both compile and test classpath (MCOBERTURA-26)
     *
     * @param artifact
     * @return re-scoped artifact
     */
    private Artifact artifactScopeToProvided( Artifact artifact )
    {
        return factory.createArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                                       Artifact.SCOPE_PROVIDED, artifact.getType() );
    }


    private Artifact getResolvedArtifact( String groupId, String artifactId, String version )
        throws ArtifactNotFoundException, ArtifactResolutionException
    {
        Artifact artifact = factory.createArtifact( groupId, artifactId, version, Artifact.SCOPE_RUNTIME, "jar" );
        resolver.resolve( artifact, remoteRepos, localRepo );
        return artifact;
    }

}