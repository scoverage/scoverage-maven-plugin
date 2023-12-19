try {

    def logFile = new File(basedir, "build.log")
    def lines = logFile.readLines()
    assert lines.contains("[WARNING] Skipping SCoverage execution - unsupported Scala version \"2.11.12\". Supported Scala versions are 2.12.8+, 2.13.0+ and 3.2.0+ .")

    return true

} catch (Throwable e) {
    e.printStackTrace()
    return false
}
