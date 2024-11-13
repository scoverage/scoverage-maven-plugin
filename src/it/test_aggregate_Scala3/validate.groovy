try {

    def module1ScoverageFile = new File(basedir, "module01/target/scoverage.xml")
    assert module1ScoverageFile.exists()

    def module1ReportFile = new File(basedir, "module01/target/site/scoverage/index.html")
    assert module1ReportFile.exists()

    def aggregatedScoverageFile = new File(basedir, "target/scoverage.xml")
    assert aggregatedScoverageFile.exists()

    def aggregatedReportFile = new File(basedir, "target/site/scoverage/index.html")
    assert aggregatedReportFile.exists()

    return true

} catch (Throwable e) {
    e.printStackTrace()
    return false
}
