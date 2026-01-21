package services.data

import org.junit.Test
import org.junit.Assert.assertEquals

class DataServiceTest {
  @Test
  def testSave(): Unit = {
    assertEquals("Saved: mydata", DataService.save("mydata"))
  }

  @Test
  def testGetVersion(): Unit = {
    assertEquals("DataService-1.0.0", DataService.getVersion)
  }
}

