package core.api

import org.junit.Test
import org.junit.Assert.assertEquals

class CoreApiTest {
  @Test
  def testVersion(): Unit = {
    assertEquals("1.0.0", CoreApi.version)
  }

  @Test
  def testName(): Unit = {
    assertEquals("CoreApi", CoreApi.name)
  }
}

