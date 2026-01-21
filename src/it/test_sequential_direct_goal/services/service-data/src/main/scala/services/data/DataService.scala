package services.data

class DataService {
  def save(data: String): String = {
    s"Saved: $data"
  }

  def getVersion: String = {
    "DataService-1.0.0"
  }
}

object DataService extends DataService

