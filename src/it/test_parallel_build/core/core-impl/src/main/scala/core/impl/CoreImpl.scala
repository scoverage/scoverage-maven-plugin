package core.impl

class CoreImpl {
  def getFullVersion: String = {
    "CoreImpl-1.0.0"
  }

  def process(input: String): String = {
    s"Processed: $input"
  }
}

object CoreImpl extends CoreImpl

