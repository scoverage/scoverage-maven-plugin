package consumer

import testlib.{TestUtils, UnitTestBase}

// This test uses test-scoped dependency on test-lib
// Without the fix, this will fail with "object testlib is not a member of package"
class CalculatorTest extends UnitTestBase {

  def testAdd(): Unit = {
    val result = Calculator.add(2, 3)
    assert(result == 5)

    // Use test utilities from test-lib
    assert(testHelper() == "test-helper-value")
    assert(TestUtils.utilityMethod() == 42)
  }

  def testMultiply(): Unit = {
    val result = Calculator.multiply(4, 5)
    assert(result == 20)
  }
}
