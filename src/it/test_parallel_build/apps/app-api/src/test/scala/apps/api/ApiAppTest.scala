package apps.api

import org.junit.Test
import org.junit.Assert.assertEquals

class ApiAppTest {
  @Test
  def testHandleRequest(): Unit = {
    assertEquals("[ApiApp] hello", ApiApp.handleRequest("hello"))
  }

  @Test
  def testGetApiName(): Unit = {
    assertEquals("ApiApp", ApiApp.getApiName)
  }
}

