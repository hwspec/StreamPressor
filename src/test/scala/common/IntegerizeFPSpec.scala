// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package common

import chisel3.simulator.ChiselSim
// Note: Formal testing (BoundedCheck, Formal) is not available in ChiselSim
// Formal tests are commented out for now
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfterAllConfigMap, ConfigMap}

// import java.lang.{Float => javaFloat}
import scala.language.postfixOps
import scala.util.Random

/**
  * To enable the formal test,
  * sbt "testOnly -- -DFORMAL=1"
  * Note: Formal testing is disabled by default. You need to add a dummy value (e.g., =1)
  * as ConfigMap requires some value.
  * 
  * Note: Formal testing is not yet supported in ChiselSim
  */
class LocalConfigSpec extends AnyFlatSpec with BeforeAndAfterAllConfigMap with ChiselSim {
  private var _formalEnabled = false
  override def beforeAll(configMap: ConfigMap) = {
    _formalEnabled = configMap.contains("FORMAL")
  }
  def formalEnabled = _formalEnabled
}

// Note: Formal testing is not yet supported in ChiselSim - this test is disabled
// class IntegerizeFPFormalSpec extends LocalConfigSpec with Formal {
//   behavior.of("IntegerizeFPFormal")
//
//   Seq(32, 64).foreach { bw =>
//     s"Check the identity of $bw-bit IntegerizeFP" should "pass" in {
//       assume(formalEnabled)
//       verify(new MapFP2UIntIdentity(bw), Seq(BoundedCheck(1)))
//     }
//   }
// }


class IntegerizeFPSpec extends AnyFlatSpec with ChiselSim {
  behavior.of("IntegerizeFP")

  val debug = false
  val nrndtests = 300
  val rnd = new Random()

  val fixedtestdata: List[Float] = List(0f, 1f, -1f, 1.0001f, 1.0002f, 1.0003f)
  val rndtestdata:   List[Float] = List.fill(nrndtests) { rnd.nextFloat() }
  val testdata = fixedtestdata.concat(rndtestdata)

  import IntegerizeFPSpecUtil._

  "MapFP2UInt" should "pass" in {
    simulate(new MapFP2UInt()) { c =>
      for (d <- testdata) {
        val uint32value: BigInt = convFloat2Bin(d)

        if (debug) print(f"$uint32value%08x => ")
        c.io.rev.poke(0)
        c.io.in.poke(uint32value)
        c.clock.step() // technically not needed
        val result = c.io.out.peek().litValue // litValue returns BigInt
        if (debug) print(f"$result%08x => ")
        val expectedVal = ifp32Forward(uint32value)
        if (result != expectedVal) fail(f"Failed to validate: $result expected ${expectedVal}")

        c.io.rev.poke(1)
        c.io.in.poke(result)
        c.clock.step() // technically not needed
        val result2 = c.io.out.peek().litValue.toLong
        if (debug) println(f"$result2%08x")
        // c.io.out.expect(uint32value)
        val decoded: Float = convBin2Float(result2)
        if (d != decoded) fail(f"Discrepancy between original and decoded value: $d and $decoded")

      }
    }
  }
}
