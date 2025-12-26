try {

    // Core modules
    def coreApiScoverageFile = new File(basedir, "core/core-api/target/scoverage.xml")
    assert coreApiScoverageFile.exists()

    def coreApiReportFile = new File(basedir, "core/core-api/target/site/scoverage/index.html")
    assert coreApiReportFile.exists()

    def coreImplScoverageFile = new File(basedir, "core/core-impl/target/scoverage.xml")
    assert coreImplScoverageFile.exists()

    def coreImplReportFile = new File(basedir, "core/core-impl/target/site/scoverage/index.html")
    assert coreImplReportFile.exists()

    // Services modules
    def serviceAuthScoverageFile = new File(basedir, "services/service-auth/target/scoverage.xml")
    assert serviceAuthScoverageFile.exists()

    def serviceAuthReportFile = new File(basedir, "services/service-auth/target/site/scoverage/index.html")
    assert serviceAuthReportFile.exists()

    def serviceDataScoverageFile = new File(basedir, "services/service-data/target/scoverage.xml")
    assert serviceDataScoverageFile.exists()

    def serviceDataReportFile = new File(basedir, "services/service-data/target/site/scoverage/index.html")
    assert serviceDataReportFile.exists()

    def serviceCommonScoverageFile = new File(basedir, "services/service-common/target/scoverage.xml")
    assert serviceCommonScoverageFile.exists()

    def serviceCommonReportFile = new File(basedir, "services/service-common/target/site/scoverage/index.html")
    assert serviceCommonReportFile.exists()

    // Apps modules
    def appCliScoverageFile = new File(basedir, "apps/app-cli/target/scoverage.xml")
    assert appCliScoverageFile.exists()

    def appCliReportFile = new File(basedir, "apps/app-cli/target/site/scoverage/index.html")
    assert appCliReportFile.exists()

    def appApiScoverageFile = new File(basedir, "apps/app-api/target/scoverage.xml")
    assert appApiScoverageFile.exists()

    def appApiReportFile = new File(basedir, "apps/app-api/target/site/scoverage/index.html")
    assert appApiReportFile.exists()

    // Aggregated report at root level
    def aggregatedScoverageFile = new File(basedir, "target/scoverage.xml")
    assert aggregatedScoverageFile.exists()

    def aggregatedReportFile = new File(basedir, "target/site/scoverage/index.html")
    assert aggregatedReportFile.exists()

    return true

} catch (Throwable e) {
    e.printStackTrace()
    return false
}

