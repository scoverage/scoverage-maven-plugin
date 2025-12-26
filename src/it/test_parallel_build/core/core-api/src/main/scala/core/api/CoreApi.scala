package core.api

class CoreApi {
  def version: String = {
    "1.0.0"
  }

  def name: String = {
    "CoreApi"
  }
}

object CoreApi extends CoreApi

