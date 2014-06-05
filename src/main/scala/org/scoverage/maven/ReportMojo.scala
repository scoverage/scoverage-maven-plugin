package org.scoverage.maven

import org.apache.maven.plugins.annotations.{Component, Mojo, ResolutionScope}
import org.apache.maven.project.MavenProject
import org.apache.maven.plugin.descriptor.PluginDescriptor
import org.apache.maven.settings.Settings
import org.apache.maven.plugin.AbstractMojo
import java.io.File
import scoverage.IOUtils
import scoverage.report.{ScoverageHtmlWriter, ScoverageXmlWriter, CoberturaXmlWriter}

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

    val coberturaDir = new File(project.getBasedir + "/target/coverage-report")
    val reportsDir = new File(project.getBasedir + "/target/scoverage-report")
    val dataDir = new File(project.getBasedir + "/target")
    val classesDir = new File(project.getBuild.getOutputDirectory)

    val coverageFile = IOUtils.coverageFile(dataDir)
    if (!coverageFile.exists()) {
       getLog.info(s"[scoverage] ${coverageFile.getAbsoluteFile} doesn't exist. Skipping report generation...")
    } else {
       val coverage = IOUtils.deserialize(coverageFile)

       val measurementFiles = IOUtils.findMeasurementFiles(dataDir)
       val measurements = IOUtils.invoked(measurementFiles)
       coverage.apply(measurements)

       getLog.info("[scoverage] Generating cobertura XML report...")
       new CoberturaXmlWriter(classesDir, coberturaDir).write(coverage)

       getLog.info("[scoverage] Generating scoverage XML report...")
       new ScoverageXmlWriter(classesDir, reportsDir, false).write(coverage)

       getLog.info("[scoverage] Generating scoverage HTML report...")
       new ScoverageHtmlWriter(classesDir, reportsDir).write(coverage)
    }
  }
}
