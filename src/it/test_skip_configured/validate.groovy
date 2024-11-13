try {

    def logFile = new File(basedir, "build.log")
    def lines = logFile.readLines()
    assert lines.contains("[INFO] Skipping Scoverage execution as configured")
    assert !lines.contains("Generating \"SCoverage\" report")

    // let's check main code is also not compiled
    def scalaMainSourcesCompilerInfoIndex = lines.findIndexOf { it ==~ /.*:compile \(default-sbt-compile\).*/ } + 1
    def scalaMainSourcesCompilerInfo = lines.get(scalaMainSourcesCompilerInfoIndex)
    def expectedInfo = "[INFO] Not compiling main sources"
    assert scalaMainSourcesCompilerInfo.contains(expectedInfo) : "Expected '$expectedInfo' but got: '$scalaMainSourcesCompilerInfo'"

    return true

} catch (Throwable e) {
    e.printStackTrace()
    return false
}
