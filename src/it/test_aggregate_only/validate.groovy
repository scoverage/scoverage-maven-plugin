try {

    def module1ScoverageFile = new File(basedir, "module01/target/scoverage.xml")
    assert !module1ScoverageFile.exists()

    def module1ReportFile = new File(basedir, "module01/target/site/scoverage/index.html")
    assert !module1ReportFile.exists()

    def module2ScoverageFile = new File(basedir, "module02/target/scoverage.xml")
    assert !module2ScoverageFile.exists()

    def module2ReportFile = new File(basedir, "module02/target/site/scoverage/index.html")
    assert !module2ReportFile.exists()

    def module3ScoverageFile = new File(basedir, "module03/target/scoverage.xml")
    assert !module3ScoverageFile.exists()

    def module3ReportFile = new File(basedir, "module03/target/site/scoverage/index.html")
    assert !module3ReportFile.exists()

    def module4ScoverageFile = new File(basedir, "module04/target/scoverage.xml")
    assert !module4ScoverageFile.exists(), "Skipped module (via custom property) should not generate scoverage.xml"

    def module4ReportFile = new File(basedir, "module04/target/site/scoverage/index.html")
    assert !module4ReportFile.exists(), "Skipped module (via custom property) should not generate report"

    def aggregatedScoverageFile = new File(basedir, "target/scoverage.xml")
    assert aggregatedScoverageFile.exists()

    def aggregatedReportFile = new File(basedir, "target/site/scoverage/index.html")
    assert aggregatedReportFile.exists()

    return true

} catch (Throwable e) {
    e.printStackTrace()
    return false
}
