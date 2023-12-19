package service

import org.scalatest.wordspec.AnyWordSpec

class HelloServiceScalaTest extends AnyWordSpec {

  "HelloService" should {
    "say hello" in {
      assert(HelloServiceScala.hello == "Hello")
    }
  }
}
