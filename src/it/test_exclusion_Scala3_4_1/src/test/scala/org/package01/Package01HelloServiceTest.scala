package org.package01

import org.scalatest.wordspec.AnyWordSpec

class Package01HelloServiceTest extends AnyWordSpec {

  "Package01HelloService" should {
    "say hello" in {
      assert(Package01HelloService.hello == "Hello from package01")
    }
  }
}
