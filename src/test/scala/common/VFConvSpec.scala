// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package common

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import Misc._
import ConvTestPats._

class VFConvSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Variable-fix Converters"

  // =======================
  // Verify V2FConv
  // =======================

  def checkInitConditionV2FConv(c: V2FConv): Unit = {
    c.io.in.initSource().setSourceClock(c.clock)
    c.io.out.initSink().setSinkClock(c.clock)
    c.io.in.ready.expect(true.B) // check the initial condition
    c.io.out.valid.expect(false.B)
  }

  "V2FConv" should "pass" in {
    test(new V2FConv(vencbusbw, fdbusbw, packetbw, p_debuglevel = 1)) {
      c => {
        checkInitConditionV2FConv(c)
        fork {
          for(i <- 0 until 16) {
            c.io.in.enqueue(0x123456789L.S)
          }
        }.fork {
          var clk = 0
          var cnt = 1
          for (i<-0 until 4) {
            while (!c.io.out.valid.peekBoolean()) {
              clk += 1
              c.clock.step()
            }
            c.io.out.ready.poke(true.B)
            val outbits = c.io.out.bits.peekInt()
            println(f"clk${clk}/cnt${cnt}  ${outbits}%32x")
            cnt += 1
            c.clock.step()
            clk += 1
          }
        }.join()
      }
    }
  }

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

      def enqdeqF2V(tp: List[Int]) : Unit = {
        val fixedbufs = genfixedfrompat(tp)
        fork {
          for(b <- fixedbufs) {
            enqF2V(c, b)
          }
        }.fork {
          var clk = 0
          var cnt = 1
          c.io.out.ready.poke(true.B)
          for(t <- tp) {
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
          while(c.io.out.valid.peekBoolean()) { // technically wrong
            val outbits =  c.io.out.bits.peekInt()
            //println(f"clk${clk}/cnt${cnt}   ${outbits}%09x rest")
            //assert(outbits == 0)
            c.clock.step()
            cnt += 1
            clk += 1
          }
          println(f"Found ${cnt-cntreststart} pads")
        }.join()
      }
      for (tp <- testpatterns)  enqdeqF2V(tp)
    }
  }




  // ==============================================================
  // The codes below will be removed
  // ==============================================================

  // XXX: replace gendummycontents with genpayloadspat. update all related codes
  def gendummycontents(len: Int): List[Int] = List.tabulate(len - 1)(i => i + 1)

  // generate software references
  def genref(tp: List[Int]): List[(List[Int], List[Int])] = {
    val tmp = tp.scanLeft((List.empty[Int], List.empty[Int])) {
      case ((buf, carryover), in) => {
        //val newpayload : List[Int] = List[Int](in) ::: List.tabulate(in-1)(i => i+1) // header + contents
        val newpayload: List[Int] = List[Int](in) ::: gendummycontents(in) // header + contents
        val prev = if (carryover.length > 0 || buf.length == nblocks) carryover else buf

        val availlen = nblocks - prev.length

        if (availlen >= newpayload.length) (prev ::: newpayload, List.empty[Int])
        else (prev ::: newpayload.take(availlen), newpayload.drop(availlen))
      }
    }
    tmp.drop(1) // drop(1) because the first element is empty
  }

  case class ExpectedValues(inputs: List[Int], expected_buf: List[List[Int]])

  val refs: List[ExpectedValues] = testpatterns.map {
    in => {
      val tmp = genref(in)
      val tmp2: List[List[Int]] = tmp.map { p => p._1 }
      ExpectedValues(in, tmp2)
    }
  }

  "ConvV2F" should "pass" in {
    test(new ConvV2F(bw = packetbw, nblocks = nblocks)) { c =>
      def testLoopWithExpectedValues(p: ExpectedValues): Unit = {
        p.inputs zip p.expected_buf foreach {
          case (in, ref) => {
            println(in, ref)
            // enqueue
            c.io.in.bits.len.poke(in)
            c.io.in.bits.data(0).poke(in)
            gendummycontents(in).foreach { d => c.io.in.bits.data(d).poke(d) }
            c.io.in.valid.poke(1)
            c.clock.step()
            // dequeue
            c.io.out.ready.poke(1)
            val valid = c.io.out.valid.peekBoolean()
            if (valid) {
              val out = c.io.out.bits.peekInt()
              val outref = castListInt2BigInt(ref, packetbw)
              println(f"${out.toString(16)} ${outref.toString(16)} ${out == outref}")
              c.io.out.bits.expect(outref)
            }
          }
        }
        c.io.in.bits.len.poke(nblocks - 1)
        for (i <- 0 until 16) {
          c.io.in.bits.data(i).poke(0)
        }
        c.io.in.valid.poke(1)
        c.clock.step()
        c.io.out.ready.poke(1)
        val valid = c.io.out.valid.peekBoolean()
        if (valid) {
          val out = c.io.out.bits.peekInt()
          println(f"${out.toString(16)}")
        }
      }
      //refs.foreach { r => testLoopWithExpectedValues(r) }
      testLoopWithExpectedValues(refs(3))
    }
  }

  "ConvV2F init condition" should "pass" in {
    test(new ConvV2F(bw = packetbw, nblocks = nblocks)) { c =>
      c.io.in.initSource().setSourceClock(c.clock)
      c.io.out.initSink().setSinkClock(c.clock)

      c.io.in.ready.expect(true.B) // check the initial condition
      c.io.out.valid.expect(false.B)

      fork {
        // fill all the blocks to generate an output
        for (_ <- 1 to nblocks) {
          while (!c.io.in.ready.peekBoolean()) c.clock.step()
          c.io.in.bits.len.poke(1)
          for (j <- 0 until nblocks) {
            if (j == 0) c.io.in.bits.data(j).poke(1)
            else c.io.in.bits.data(j).poke(0)
          }
          c.io.in.valid.poke(true.B)
          c.clock.step()
        }
      }.fork {
        c.io.out.expectDequeue(
          castListInt2BigInt(List.fill(nblocks){1}, packetbw).U)
      }.join()
    }
  }
}
