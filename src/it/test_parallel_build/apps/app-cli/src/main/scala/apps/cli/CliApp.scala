package apps.cli

class CliApp {
  def run(user: String, password: String, input: String): String = {
    if (user.nonEmpty && password.nonEmpty) {
      s"Processed: $input"
    } else {
      "Authentication failed"
    }
  }

  def getVersionInfo: String = {
    "CLI Application 1.0.0"
  }
}

object CliApp extends CliApp

