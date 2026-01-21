package apps.cli

import org.junit.Test
import org.junit.Assert.assertEquals

class CliAppTest {
  @Test
  def testRunSuccess(): Unit = {
    assertEquals("Processed: data", CliApp.run("user", "pass", "data"))
  }

  @Test
  def testRunFail(): Unit = {
    assertEquals("Authentication failed", CliApp.run("", "pass", "data"))
  }

  @Test
  def testGetVersionInfo(): Unit = {
    assertEquals("CLI Application 1.0.0", CliApp.getVersionInfo)
  }
}

