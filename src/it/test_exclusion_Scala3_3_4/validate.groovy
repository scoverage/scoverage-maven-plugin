try {

    def logFile = new File(basedir, "build.log")
    def lines = logFile.readLines()
    assert lines.contains("[INFO] Statement coverage.: 100.00%")
    assert lines.contains("[INFO] Branch coverage....: 100.00%")

    def scoverageFile = new File(basedir, "target/scoverage.xml")
    assert scoverageFile.exists()

    def reportFile = new File(basedir, "target/site/scoverage/index.html")
    assert reportFile.exists()

    return true

} catch (Throwable e) {
    e.printStackTrace()
    return false
}
