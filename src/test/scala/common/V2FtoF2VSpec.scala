// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package common

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random
import scala.collection.mutable.ListBuffer

import Misc._

/**
 * Inject data into V2F that is connected to F2V, which should output the data injected into V2F.
 *
 * Todo:
 * - add expect
 * - add different test patterns
 */
class V2FtoF2VSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Loopback test"

  val encbusbw: Int = 36
  val fixbusbw: Int = 128
  val packetbw: Int = 4
  val debuglevel:Int = 0

  def checkInitConditionV2FConv(c: V2FtoF2VTest): Unit = {
    c.io.in.initSource().setSourceClock(c.clock)
    c.io.out.initSink().setSinkClock(c.clock)
    c.io.in.ready.expect(true.B) // check the initial condition
    c.io.out.valid.expect(false.B)
  }

  "Loopback test" should "pass" in {
    test(new V2FtoF2VTest(encbusbw, fixbusbw, packetbw, debuglevel)) {
      c => {
        checkInitConditionV2FConv(c)

        fork {
          for(i <- 0 until 16) {
            c.io.in.enqueue(0x2.S)
          }
        }.fork {
          var clk = 0
          var cnt = 1
          for (i<-0 until 16) {
            while (!c.io.out.valid.peekBoolean()) {
              clk += 1
              c.clock.step()
            }
            c.io.out.ready.poke(true.B)
            val outbits = c.io.out.bits.peekInt()
            println(f"clk${clk}/cnt${cnt} ${outbits}")
            cnt += 1
            c.clock.step()
            clk += 1
          }
        }.join()
      }
    }
  }
}
