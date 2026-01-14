// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package lpe

import chisel3.simulator.ChiselSim
// Note: Formal testing (BoundedCheck, Formal) is not available in ChiselSim
// Formal tests are commented out for now
import common.LocalConfigSpec
import org.scalatest.flatspec.AnyFlatSpec

/** *
 * LPCompFormalSpec runs formal test for LPCompIdentity().
 *
 * Note: Formal testing is not yet supported in ChiselSim - this test is disabled
 */
// class LPCompFormalSpec extends LocalConfigSpec with Formal {
//   behavior of "LPCompFormal"
//
//   "Check the identity of LPComp" should "pass" in {
//     assume(formalEnabled)
//     verify(new LPCompIdentity(), Seq(BoundedCheck(1)))
//   }
// }

class LPEncoderSpec extends AnyFlatSpec with ChiselSim {
  behavior of "LPEncoder"

  val bw = 32
  val pred = List(4, -6, 4, -1)
  val tp = new LagrangePredTestPatterns(bw, pred)

  def report_bitusage(orig: BigInt, sign: BigInt, m: BigInt): Unit = {
    val predicted = if (sign > 0) -m else m
    val diff = orig - predicted // both are BigInt
    val nbits = diff.bitLength + 1 // .bitLength returns the bit length without the sign bit
    println(f"nbits=${nbits + 1}%2d  ${orig} - $predicted => $diff")
  }

  def testLoop(inputs: List[BigInt], refs: List[BigInt], verbose: Boolean = false): Unit = {
    simulate(new LPEncoder(bw, fpmode = false, coefficients = tp.lagrangepred)) { c =>
      inputs.zip(refs) foreach { d =>
        c.io.in_data.poke(d._1)
        c.clock.step()
        if (verbose) report_bitusage(d._1, c.io.out_sign.peek().litValue, c.io.out_data.peek().litValue)
        val expectedval = d._1 - d._2
        c.io.out_data.expect(expectedval.abs)
        val sign = if (expectedval < 0) 1 else 0
        c.io.out_sign.expect(sign)
        // c.io.out_nbits.expect(expectedval.abs.bitLength)
      }
    }
  }

  "fixed int" should "pass" in testLoop(tp.inputs_fixed_int, tp.refs_fixed_int)

  "max int" should "pass" in testLoop(tp.inputs_max_int, tp.refs_max_int)

  "linear int" should "pass" in testLoop(tp.inputs_lin_int, tp.refs_lin_int)

  "randomized int" should "pass" in testLoop(tp.inputs_rnd_int, tp.refs_rnd_int)

  def testLoopIdentity(inputs: List[BigInt], refs: List[BigInt], verbose: Boolean = false): Unit = {
    def loophelper(c: LPCompIdentity) : Unit = {
      inputs.zip(refs) foreach { d =>
        c.in.poke(d._1)
        c.clock.step()
        if (verbose) println(f"${d._1} ${c.out.peek().litValue}")
        c.out.expect(d._1)
      }
      c.clock.step()
    }

    simulate(new LPCompIdentity(bw, fpmode = false, coefficients = tp.lagrangepred)) { c => loophelper(c) }

    // Note: fpmode = true means that IntegerizedFP is inserted before encoding or after decoding.
    simulate(new LPCompIdentity(bw, fpmode = true, coefficients = tp.lagrangepred)) { c => loophelper(c) }
  }

  "LPComp: fixed int" should "pass" in testLoopIdentity(tp.inputs_fixed_int, tp.refs_fixed_int)

  "LPComp: max int" should "pass" in testLoopIdentity(tp.inputs_max_int, tp.refs_max_int)

  "LPComp: linear int" should "pass" in testLoopIdentity(tp.inputs_lin_int, tp.refs_lin_int)

  "LPComp: randomized int" should "pass" in testLoopIdentity(tp.inputs_rnd_int, tp.refs_rnd_int)
}
