package testlib

trait UnitTestBase {
  def testHelper(): String = "test-helper-value"
}

object TestUtils {
  def utilityMethod(): Int = 42
}
