package core.impl

import org.junit.Test
import org.junit.Assert.assertEquals

class CoreImplTest {
  @Test
  def testGetFullVersion(): Unit = {
    assertEquals("CoreImpl-1.0.0", CoreImpl.getFullVersion)
  }

  @Test
  def testProcess(): Unit = {
    assertEquals("Processed: test", CoreImpl.process("test"))
  }
}

