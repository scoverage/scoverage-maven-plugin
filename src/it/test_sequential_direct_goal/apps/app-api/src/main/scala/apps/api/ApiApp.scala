package apps.api

class ApiApp {
  def handleRequest(request: String): String = {
    s"[ApiApp] $request"
  }

  def getApiName: String = {
    "ApiApp"
  }
}

object ApiApp extends ApiApp

