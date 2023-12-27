try {

    def aggregatedScoverageFile = new File(basedir, "target/scoverage.xml")
    assert aggregatedScoverageFile.exists()

    def aggregatedReportFile = new File(basedir, "target/site/scoverage/index.html")
    assert aggregatedReportFile.exists()

    return true

} catch (Throwable e) {
    e.printStackTrace()
    return false
}
