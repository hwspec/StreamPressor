// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package lpe

import chisel3._
//import chisel3.util.log2Ceil
import chisel3.simulator.ChiselSim
// Note: Formal testing (BoundedCheck, Formal) is not available in ChiselSim
// Formal tests are commented out for now
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.Tag
import common.LocalConfigSpec

import scala.util.Random

// Note: Formal testing is not yet supported in ChiselSim - this test is disabled
// class LagrangePredFormalSpec extends LocalConfigSpec with Formal {
//   behavior.of("LagrangePredFormal")
//
//   "Check the identity of LagrangePred" should "pass" in {
//     assume(formalEnabled)
//     verify(new LagrangePredIdentity(), Seq(BoundedCheck(10))) // 10 cycles
//   }
// }

class LagrangePredTestPatterns(val bw: Int = 32, val lagrangepred: List[Int] = List(4, -6, 4, -1)) {
  require(bw >= 10 && bw <= 64) // due to hard-coded values for inputs_fixed_int and nextLong for inputs_rnd_int
  //val lagrangepred: List[Int] = List(4, -6, 4, -1)
  val nlagrangepred = lagrangepred.length
  val padding: List[BigInt] = List.fill(nlagrangepred)(0)
  val rnd = new Random()
  val maxval = (BigInt(1) << bw) - 1 // max value of bw-bit unsigned int

  val inputs_fixed_int: List[BigInt] =
    padding ::: List[BigInt](512, 532, 513, 514, 513, 512, 509, 508, 507, 505, 500, 504, 512, 511, 510)
  val inputs_max_int: List[BigInt] = padding ::: List.fill(8)(maxval)
  val inputs_lin_int: List[BigInt] = padding ::: List.tabulate(10)(i => BigInt(i))
  val inputs_rnd_int: List[BigInt] = padding ::: List.fill(10)(BigInt(rnd.nextLong(maxval.toLong)))

  // XXX: replace genReferences with performLagrangeForward
  def genReferences(inputs: List[BigInt]): List[BigInt] = {
    padding ::: inputs
      .sliding(lagrangepred.length)
      .map { w =>
        val z = w.zip(lagrangepred.reverse)
        z.foldLeft(BigInt(0)) { case (acc, (c, v)) => acc + c * v }
      }
      .toList
      .drop(1)
  }

  // generate software reference for each input
  val refs_fixed_int = genReferences(inputs_fixed_int)
  val refs_lin_int = genReferences(inputs_lin_int)
  val refs_rnd_int = genReferences(inputs_rnd_int)
  val refs_max_int = genReferences(inputs_max_int)
}

@Tag("RequiresVerilator")
class LagrangePredSpec extends AnyFlatSpec with ChiselSim {
  behavior.of("LagrangePredSpec")

  val bw = 32
  val pred = List(4, -6, 4, -1)
  val tp = new LagrangePredTestPatterns(bw, pred)

  def report_bitusage(orig: BigInt, predicted: BigInt, expected: BigInt): Unit = {
    val diff = orig - predicted // both are BigInt
    val nbits = diff.bitLength + 1 // .bitLength returns the bit length without the sign bit
    println(f"nbits=${nbits + 1}%2d  ${orig} - $predicted (= $expected} => $diff")
  }

  def testLoop(inputs: List[BigInt], refs: List[BigInt], verbose: Boolean = false): Unit = {
    simulate(new LagrangePred(bw, coefficients = tp.lagrangepred)) { c =>
      inputs.zip(refs).foreach { d =>
        c.io.in.poke(d._1)
        c.clock.step()
        c.io.out.expect(d._2)
        if (verbose) report_bitusage(d._1, c.io.out.peek().litValue, d._2)
      }
    }
  }

  "fixed int" should "pass" in testLoop(tp.inputs_fixed_int, tp.refs_fixed_int)

  "max int" should "pass" in testLoop(tp.inputs_max_int, tp.refs_max_int)

  "linear int" should "pass" in testLoop(tp.inputs_lin_int, tp.refs_lin_int)

  "randomized int" should "pass" in testLoop(tp.inputs_rnd_int, tp.refs_rnd_int)

  //
  //
  //

  def testLoopIdentity(inputs: List[BigInt], refs: List[BigInt], verbose: Boolean = false): Unit = {
    simulate(new LagrangePredIdentity(bw, coefficients = tp.lagrangepred)) { c =>
      inputs.zip(refs).foreach { d =>
        c.in.poke(d._1)
        c.clock.step()
        if (verbose) println(f"in=${d._1} out=${c.out.peek().litValue}")
        c.out.expect(d._1)
      }
    }
  }

  "Identity fixed int" should "pass" in testLoopIdentity(tp.inputs_fixed_int, tp.refs_fixed_int)
}

/*
import org.scalatest.run
object localtest extends App {
  run(new LagrangePredSpec)
}
 */
