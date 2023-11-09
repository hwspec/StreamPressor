// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package common

import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ClzParamSpec extends AnyFlatSpec with ChiselScalatestTester {

  behavior.of("ClzParam")

  val nb = 32 // the size of input data in bits

  val seed = 123
  val rn = new scala.util.Random(seed)

  def genNumber(): (Long, Int) = {
    val sh = rn.nextInt(nb + 1) // random number between 0 and nb
    val b = if (sh == nb) 0.toLong else 1.toLong << sh
    val l = if (b < 2.toLong) 0.toLong else rn.nextLong().abs % b
    (b + l, sh)
  }
  val ntries = 100

  def toBinStr(v: Long): String = {
    val s = v.toBinaryString
    val zslen = nb - s.length()
    val zs = "0" * zslen
    zs + s
  }

  "ClzParam" should "pass" in {
    test(new ClzParam(nb)) { c =>
      for (_ <- 0 until ntries) {
        val (in, sh) = genNumber()
        //val str = toBinStr(in)
        val ref = if (sh == nb) nb else nb - sh - 1
        //print("in=" + str)
        c.io.in.poke(in)
        c.io.out.expect(ref)
        //val out = c.io.out.peek()
        //print(f"  out=$out\n")
        c.clock.step()
      }
    }
  }
}
