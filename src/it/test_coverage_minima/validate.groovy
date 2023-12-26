def checkModule(logText, module, coverageLog) {
    assert new File(basedir, module + "/target/scoverage.xml").exists()
    assert new File(basedir, module + "/target/site/scoverage/index.html").exists()
    def entry = logText.find {
        it.startsWith("scoverage") &&
        it.contains(":check (default-cli) @ " + module + " ---\n")
    }
    assert entry != null
    assert entry.endsWith(" @ " + module + " ---" + coverageLog.replaceAll("\r\n", "\n") + "[INFO] ")
}

try {
    // check coverage minima
    def logText = (new File(basedir, "build.log")).text.replaceAll("\r\n", "\n").split(/\n\[INFO\] \-\-\- /)

    checkModule(logText, "module01",
            """
            |[INFO] Statement coverage.: 100.00%
            |[INFO] Branch coverage....: 100.00%
            |[INFO] Coverage is above minimum [100.00% >= 95.00%]
            |""".stripMargin()
    )
    checkModule(logText, "module02",
            """
            |[INFO] Statement coverage.: 50.00%
            |[INFO] Branch coverage....: 100.00%
            |[ERROR] Coverage is below minimum [50.00% < 95.00%]
            |""".stripMargin()
    )
    checkModule(logText, "module03",
            """
            |[INFO] Statement coverage.: 100.00%
            |[INFO] Branch coverage....: 100.00%
            |[INFO] 100% Coverage !
            |""".stripMargin()
    )

    return true

} catch (Throwable e) {
    e.printStackTrace()
    return false
}
