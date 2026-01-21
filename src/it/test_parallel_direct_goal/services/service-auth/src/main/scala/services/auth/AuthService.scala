package services.auth

class AuthService {
  def authenticate(user: String, password: String): Boolean = {
    user.nonEmpty && password.nonEmpty
  }

  def getServiceInfo: String = {
    "AuthService-1.0.0"
  }
}

object AuthService extends AuthService

