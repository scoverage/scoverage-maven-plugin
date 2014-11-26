import org.scalatest._ 
import org.scalatest.Matchers._

/**
 * Created by tbarke001c on 7/8/14.
 */
class GoodCoverageSpec extends WordSpec {

  "GoodCoverage" should {
    "sum two numbers" in {
      GoodCoverage.sum(1, 2) should equal (3)
    }
  }
}
