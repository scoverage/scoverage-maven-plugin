package services.common

import org.junit.Test
import org.junit.Assert.assertEquals

class CommonServiceTest {
  @Test
  def testGetApiName(): Unit = {
    assertEquals("CommonService", CommonService.getApiName)
  }

  @Test
  def testFormat(): Unit = {
    assertEquals("[CommonService] hello", CommonService.format("hello"))
  }
}

