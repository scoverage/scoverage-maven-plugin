package org.scalescc.maven

import org.apache.maven.plugins.annotations.{Component, Mojo, ResolutionScope}
import org.apache.maven.project.MavenProject
import org.apache.maven.plugin.descriptor.PluginDescriptor
import org.apache.maven.settings.Settings
import org.apache.maven.plugin.AbstractMojo
import scales.InstrumentationRuntime

/** @author Stephen Samuel */
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
    getLog.info("Coverage" + System.identityHashCode(InstrumentationRuntime.coverage))
    getLog.info("Statements: " + InstrumentationRuntime.coverage.statements)
    getLog.info("Writing report [todo]")
  }
}
