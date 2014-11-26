import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith

/**
 * Created by tbarke001c on 7/8/14.
 */
@RunWith(classOf[JUnitRunner])
class GoodCoverageSpec extends Specification {

  "GoodCoverage" should {
    "sum two numbers" in {
      GoodCoverage.sum(1, 2) mustEqual 3
    }
  }
}
