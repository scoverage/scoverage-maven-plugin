package services.auth

import org.junit.Test
import org.junit.Assert._

class AuthServiceTest {
  @Test
  def testAuthenticateSuccess(): Unit = {
    assertTrue(AuthService.authenticate("user", "pass"))
  }

  @Test
  def testAuthenticateFail(): Unit = {
    assertFalse(AuthService.authenticate("", "pass"))
  }

  @Test
  def testGetServiceInfo(): Unit = {
    assertEquals("AuthService-1.0.0", AuthService.getServiceInfo)
  }
}

