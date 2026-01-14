// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package common

import chisel3._
import chisel3.simulator.ChiselSim
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{Ignore, Tag}
import Misc._
import ConvTestPats._

@Tag("RequiresVerilator")
class F2VConvSpec extends AnyFlatSpec with ChiselSim {
  behavior of "Fix-Variable Converters"

  // ==============================================================
  // test for F2VConv
  // XXX: create a separate spec (F2VConvSpec) later.
  //      test pattern generators needs to be moved to a separate
  //      class that can be shared by both V2FConv and F2VConv.
  // ==============================================================
  def enqF2V(c: F2VConv, b: BigInt): Unit = {
    var clk = 0
    while (!c.io.in.ready.peek().litToBoolean) {
      clk += 1
      c.clock.step()
    }
    println(f"Waited ${clk} clock(s) for in.ready")
    println(f"input: ${biginthexstr(b, fdbusbw)}")
    c.io.in.bits.poke(b.U)
    c.io.in.valid.poke(true.B)
    c.clock.step(1)
    c.io.in.valid.poke(false.B)
    c.clock.step(1)
  }


// IGNORE THIS TEST FOR NOW
  "F2VConv multiple patterns" should "pass" ignore {
    def checkInitCondition(c: F2VConv): Unit = {
      // ChiselSim doesn't have initSource/initSink, just check initial state
      assert(c.io.in.ready.peek().litToBoolean, "in.ready should be true initially")
      assert(!c.io.out.valid.peek().litToBoolean, "out.valid should be false initially")
    }

    simulate(new F2VConv(fdbusbw, vencbusbw, packetbw, debuglevel = 0)) { c =>
      checkInitCondition(c)

      def enqdeqF2V(tp: List[Int]): Unit = {
        val fixedbufs = genoutputfromtestpat(tp)
        // ChiselSim doesn't support fork - rewrite as sequential
        // Strategy: enqueue all inputs first, then dequeue all outputs
        c.io.out.ready.poke(true.B)
        
        // First, enqueue all inputs (waiting for ready if needed)
        for (b <- fixedbufs) {
          // Wait for ready
          while (!c.io.in.ready.peek().litToBoolean) {
            c.clock.step()
          }
          c.io.in.bits.poke(b.U)
          c.io.in.valid.poke(true.B)
          c.clock.step()
          c.io.in.valid.poke(false.B)
        }
        
        // Then, dequeue all expected outputs
        var outputCnt = 0
        var clk = 0
        val maxCycles = 10000
        
        // Wait for and read all expected outputs
        while (outputCnt < tp.length && clk < maxCycles) {
          if (c.io.out.valid.peek().litToBoolean) {
            val outbits = c.io.out.bits.peek().litValue.toInt
            val ref = genpayloadval(tp(outputCnt))
            assert(outbits == ref, f"out:${outbits}%09x should be ref:${ref}%09x at output ${outputCnt}")
            c.clock.step()
            outputCnt += 1
            clk += 1
          } else {
            c.clock.step()
            clk += 1
          }
        }
        
        // Read any remaining outputs (padding)
        while (c.io.out.valid.peek().litToBoolean && clk < maxCycles) {
          c.clock.step()
          clk += 1
        }
        
        if (clk >= maxCycles) {
          fail(f"Test exceeded maximum cycles (${maxCycles})")
        }
        
        assert(outputCnt == tp.length, f"Expected ${tp.length} outputs but got ${outputCnt}")
        println(f"Processed ${fixedbufs.length} inputs, ${outputCnt} outputs in ${clk} cycles")
      }

      // Process each test pattern separately
      for (tp <- testpatterns) {
        // Reset state by creating a fresh simulation context for each pattern
        // Note: ChiselSim doesn't support reset, so we rely on the module's initial state
        enqdeqF2V(tp)
      }
    }
  }
}
