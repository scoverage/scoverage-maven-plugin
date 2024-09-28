try {

    def logFile = new File(basedir, "build.log")
    def lines = logFile.readLines()
    assert lines.contains("[WARNING] Package exclusion is supported for Scala [3.3.4-3.4.0) or 3.4.2+")
    assert lines.contains("[WARNING] File exclusion is supported for Scala [3.3.4-3.4.0) or 3.4.2+")
    assert lines.contains("[INFO] Statement coverage.: 10.00%")

    return true

} catch (Throwable e) {
    e.printStackTrace()
    return false
}
