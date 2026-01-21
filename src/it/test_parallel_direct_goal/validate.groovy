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

    // Skipped modules should NOT generate reports
    def coreUtilSkippedScoverageFile = new File(basedir, "core/core-util-skipped/target/scoverage.xml")
    assert !coreUtilSkippedScoverageFile.exists(), "Skipped module should not generate scoverage.xml"

    def coreUtilSkippedReportFile = new File(basedir, "core/core-util-skipped/target/site/scoverage/index.html")
    assert !coreUtilSkippedReportFile.exists(), "Skipped module should not generate report"

    def serviceUtilSkippedScoverageFile = new File(basedir, "services/service-util-skipped/target/scoverage.xml")
    assert !serviceUtilSkippedScoverageFile.exists(), "Skipped module should not generate scoverage.xml"

    def serviceUtilSkippedReportFile = new File(basedir, "services/service-util-skipped/target/site/scoverage/index.html")
    assert !serviceUtilSkippedReportFile.exists(), "Skipped module should not generate report"

    def appAdminSkippedScoverageFile = new File(basedir, "apps/app-admin-skipped/target/scoverage.xml")
    assert !appAdminSkippedScoverageFile.exists(), "Skipped module (via custom property) should not generate scoverage.xml"

    def appAdminSkippedReportFile = new File(basedir, "apps/app-admin-skipped/target/site/scoverage/index.html")
    assert !appAdminSkippedReportFile.exists(), "Skipped module (via custom property) should not generate report"

    // Aggregated report at root level
    def aggregatedScoverageFile = new File(basedir, "target/scoverage.xml")
    assert aggregatedScoverageFile.exists()

    def aggregatedReportFile = new File(basedir, "target/site/scoverage/index.html")
    assert aggregatedReportFile.exists()

    // Parse aggregated report
    def aggregatedXml = new groovy.xml.XmlSlurper().parse(aggregatedScoverageFile)
    def statementCount = aggregatedXml.@'statement-count'.toInteger()
    def statementsInvoked = aggregatedXml.@'statements-invoked'.toInteger()
    def statementRate = aggregatedXml.@'statement-rate'.toDouble()

    def packages = aggregatedXml.packages.package
    def packageNames = packages.collect { it.@name.toString() }.sort()

    // Expected values
    def expectedStatements = 15
    def expectedStatementsInvoked = 15
    def expectedCoverage = 100.0
    def expectedPackages = [
            'apps.api', 'apps.cli',
            'core.api', 'core.impl',
            'services.auth', 'services.common', 'services.data'
    ].sort()

    // Validate statement count
    assert statementCount == expectedStatements,
            "Wrong statement count: expected ${expectedStatements}, got ${statementCount}"

    // Validate statements invoked (catches aggregation bugs)
    assert statementsInvoked == expectedStatementsInvoked,
            "Wrong statements invoked: expected ${expectedStatementsInvoked}, got ${statementsInvoked} (${statementRate}% coverage)"

    // Validate coverage rate
    assert statementRate == expectedCoverage,
            "Wrong coverage rate: expected ${expectedCoverage}%, got ${statementRate}%"

    // Validate package count
    assert packageNames.size() == expectedPackages.size(),
            "Wrong package count: expected ${expectedPackages.size()}, got ${packageNames.size()}"

    // Validate all packages present
    def missingPackages = expectedPackages - packageNames
    assert missingPackages.isEmpty(),
            "Missing packages: ${missingPackages}"

    println ":white_check_mark: Aggregated report validated: ${statementCount} statements, ${statementsInvoked} invoked, ${statementRate}% coverage, ${packageNames.size()} packages"

} catch (Throwable e) {
    e.printStackTrace()
    return false
}

return true