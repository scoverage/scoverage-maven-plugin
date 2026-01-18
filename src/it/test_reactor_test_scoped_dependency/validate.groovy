def buildLog = new File(basedir, 'build.log')
def logText = buildLog.text

// Should NOT contain test compilation errors
assert !logText.contains('object testlib is not a member of package'),
    "Test failed: consumer could not find test-lib package (test-scoped dependency not resolved)"
assert !logText.contains('not found: type UnitTestBase'),
    "Test failed: consumer could not find UnitTestBase from test-lib"
assert !logText.contains('not found: type TestUtils'),
    "Test failed: consumer could not find TestUtils from test-lib"

// Should successfully compile and build
assert logText.contains('BUILD SUCCESS'), "Build should succeed"

// Verify test-lib went through scoverage instrumentation
def testLibScoverageClasses = new File(basedir, 'test-lib/target/scoverage-classes')
assert testLibScoverageClasses.exists(), "test-lib should have scoverage-classes directory"
assert testLibScoverageClasses.isDirectory(), "test-lib scoverage-classes should be a directory"

def testLibClass = new File(basedir, 'test-lib/target/scoverage-classes/testlib/TestUtils$.class')
assert testLibClass.exists(), "test-lib classes should be in scoverage-classes"

// Verify consumer compiled its tests (in forked lifecycle)
def consumerTestClasses = new File(basedir, 'consumer/target/test-classes')
assert consumerTestClasses.exists(), "consumer should have compiled test classes"

def consumerTestClass = new File(basedir, 'consumer/target/test-classes/consumer/CalculatorTest.class')
assert consumerTestClass.exists(), "consumer test should have compiled successfully"

// Verify scoverage report was generated for consumer
def consumerCoverage = new File(basedir, 'consumer/target/scoverage-data/scoverage.coverage')
assert consumerCoverage.exists(), "consumer should have scoverage coverage data"

println "✓ Test passed: test-scoped reactor dependency resolved correctly in forked lifecycle"
return true
