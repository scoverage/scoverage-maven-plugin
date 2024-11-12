try {

    def logFile = new File(basedir, "build.log")
    def lines = logFile.readLines()
    assert lines.contains("[INFO] Skipping Scoverage execution as configured")
    assert !lines.contains("Generating \"SCoverage\" report")

    return true

} catch (Throwable e) {
    e.printStackTrace()
    return false
}
