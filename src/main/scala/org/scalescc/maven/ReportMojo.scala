package org.scalescc.maven

import org.apache.maven.plugins.annotations.{Component, Mojo, ResolutionScope}
import org.apache.maven.project.MavenProject
import org.apache.maven.plugin.descriptor.PluginDescriptor
import org.apache.maven.settings.Settings
import org.apache.maven.plugin.AbstractMojo
import java.io.File
import scales.{Env, IOUtils}
import org.scalescc.reporters.{ScalesHtmlWriter, CoberturaXmlWriter, ScalesXmlWriter}

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
    val coverage = IOUtils.deserialize(Env.coverageFile)
    val measurements = IOUtils.invoked(Env.measurementFile)

    coverage.apply(measurements)

    val targetDirectory = new File("target/coverage-report")
    targetDirectory.mkdirs()

    getLog.info("Generating ScalesXML report...")
    ScalesXmlWriter.write(coverage, targetDirectory)

    getLog.info("Generating CoberturaXML report...")
    CoberturaXmlWriter.write(coverage, targetDirectory)

    getLog.info("Generating Scales HTML report...")
    ScalesHtmlWriter.write(coverage, targetDirectory)
  }
}
