package org.scalescc.maven

import org.apache.maven.plugins.annotations.{Component, Mojo, ResolutionScope}
import org.apache.maven.project.MavenProject
import org.apache.maven.plugin.descriptor.PluginDescriptor
import org.apache.maven.settings.Settings
import org.apache.maven.plugin.AbstractMojo
import scales.{Env, IOUtils}

/** @author Stephen Samuel */
@Mojo(name = "prepare",
  threadSafe = false,
  defaultPhase = org.apache.maven.plugins.annotations.LifecyclePhase.INITIALIZE)
class PrepareMojo extends AbstractMojo {

  val coverageFile: String = "target/scales.coverage"
  val measurementFile: String = "target/scales.measurement"

  def execute() {
    getLog.info(s"Using Scales coverage file [$coverageFile]")
    System.setProperty("scales.coverage.file", coverageFile)

    getLog.info(s"Using Scales measurement file [$measurementFile]")
    System.setProperty("scales.measurement.file", measurementFile)
  }
}

@Mojo(name = "report",
  threadSafe = false,
  requiresDependencyResolution = ResolutionScope.TEST,
  defaultPhase = org.apache.maven.plugins.annotations.LifecyclePhase.TEST)
class ReportMojo extends AbstractMojo {

  @Component
  var project: MavenProject = _

  @Component
  var plugin: PluginDescriptor = _

  @Component
  var settings: Settings = _

  def execute() {
    getLog.info("Creating report")

    val coverage = IOUtils.deserialize(Env.coverageFile)
    val measurements = IOUtils.invoked(Env.measurementFile)
    getLog.info("measurements: " + measurements)

    coverage.apply(measurements)

    getLog.info("Statements: " + coverage.statements)
    getLog.info("Writing report [todo]")
  }
}
