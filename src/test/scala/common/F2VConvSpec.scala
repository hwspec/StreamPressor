// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package common

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import Misc._
import ConvTestPats._

class F2VConvSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Fix-Variable Converters"

  // ==============================================================
  // test for F2VConv
  // XXX: create a separate spec (F2VConvSpec) later.
  //      test pattern generators needs to be moved to a separate
  //      class that can be shared by both V2FConv and F2VConv.
  // ==============================================================
  def enqF2V(c: F2VConv, b: BigInt): Unit = {
    var clk = 0
    while (!c.io.in.ready.peekBoolean()) {
      clk += 1
      c.clock.step()
    }
    println(f"Waited ${clk} clock(s) for in.ready")
    println(f"input: ${biginthexstr(b, fdbusbw)}")
    c.io.in.bits.poke(b)
    c.io.in.valid.poke(true.B)
    c.clock.step(1)
    c.io.in.valid.poke(false.B)
    c.clock.step(1)
  }

  "F2VConv multiple patterns" should "pass" in {
    def checkInitCondition(c: F2VConv): Unit = {
      c.io.in.initSource().setSourceClock(c.clock)
      c.io.out.initSink().setSinkClock(c.clock)
      c.io.in.ready.expect(true.B) // check the initial condition
      c.io.out.valid.expect(false.B)
    }

    test(new F2VConv(fdbusbw, vencbusbw, packetbw, debuglevel = 0)) { c =>
      checkInitCondition(c)

      def enqdeqF2V(tp: List[Int]): Unit = {
        val fixedbufs = genfixedfrompat(tp)
        fork {
          for (b <- fixedbufs) {
            enqF2V(c, b)
          }
        }.fork {
          var clk = 0
          var cnt = 1
          c.io.out.ready.poke(true.B)
          for (t <- tp) {
            while (!c.io.out.valid.peekBoolean()) {
              clk += 1
              c.clock.step()
            }
            val outbits = c.io.out.bits.peekInt()
            val ref = genpayloadval(t)
            assert(outbits == ref, f"out:${outbits}%09x should be ref:${ref}%09x")

            //println(f"clk${clk}/cnt${cnt}  ${outbits}%09x ref=${ref}%x")
            cnt += 1
            c.clock.step()
            clk += 1
          }
          // read the rest

          val cntreststart = cnt
          while (c.io.out.valid.peekBoolean()) { // technically wrong
            val outbits = c.io.out.bits.peekInt()
            //println(f"clk${clk}/cnt${cnt}   ${outbits}%09x rest")
            //assert(outbits == 0)
            c.clock.step()
            cnt += 1
            clk += 1
          }
          println(f"Found ${cnt - cntreststart} pads")
        }.join()
      }

      for (tp <- testpatterns) enqdeqF2V(tp)
    }
  }
}
