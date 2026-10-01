try {

    def logFile = new File(basedir, "build.log")
    def logText = logFile.text

    assert logText.contains("[INFO] Skipping Scoverage execution as configured")
    assert !logText.contains("Generating \"SCoverage\" report")

    // let's check main code is also not compiled
    def sbtCompileSection = logText.split("--- ").find { it.contains(":compile (default-sbt-compile)") }
    assert sbtCompileSection != null : "Did not find scala:compile (default-sbt-compile) in build.log"
    assert sbtCompileSection.contains("[INFO] Not compiling main sources")

    assert !new File(basedir, "target/classes").exists()

    return true

} catch (Throwable e) {
    e.printStackTrace()
    return false
}
