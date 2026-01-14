// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package common

import chisel3._
import chisel3.simulator.ChiselSim
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
class V2FtoF2VSpec extends AnyFlatSpec with ChiselSim {
  behavior of "Loopback test"

  val encbusbw: Int = 36
  val fixbusbw: Int = 128
  val packetbw: Int = 4
  val debuglevel:Int = 0

  def checkInitConditionV2FConv(c: V2FtoF2VTest): Unit = {
    // ChiselSim doesn't have initSource/initSink, just check initial state
    assert(c.io.in.ready.peek().litToBoolean, "in.ready should be true initially")
    assert(!c.io.out.valid.peek().litToBoolean, "out.valid should be false initially")
  }

  "Loopback test" should "pass" in {
    simulate(new V2FtoF2VTest(encbusbw, fixbusbw, packetbw, debuglevel)) {
      c => {
        checkInitConditionV2FConv(c)

        // ChiselSim doesn't support fork - rewrite as sequential
        c.io.out.ready.poke(true.B)
        var clk = 0
        var cnt = 1
        var inputIdx = 0
        
        // Process inputs and outputs sequentially
        while (inputIdx < 16 || c.io.out.valid.peek().litToBoolean) {
          // Enqueue next input if ready and available
          if (inputIdx < 16 && c.io.in.ready.peek().litToBoolean) {
            c.io.in.bits.poke(0x2.S)
            c.io.in.valid.poke(true.B)
            c.clock.step()
            c.io.in.valid.poke(false.B)
            inputIdx += 1
          }
          
          // Dequeue output if valid
          if (c.io.out.valid.peek().litToBoolean && cnt <= 16) {
            val outbits = c.io.out.bits.peek().litValue.toInt
            println(f"clk${clk}/cnt${cnt} ${outbits}")
            cnt += 1
            c.clock.step()
            clk += 1
          } else {
            c.clock.step()
            clk += 1
          }
        }
      }
    }
  }
}
