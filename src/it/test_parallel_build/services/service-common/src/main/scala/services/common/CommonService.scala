package services.common

class CommonService {
  def getApiName: String = {
    "CommonService"
  }

  def format(value: String): String = {
    s"[CommonService] $value"
  }
}

object CommonService extends CommonService

