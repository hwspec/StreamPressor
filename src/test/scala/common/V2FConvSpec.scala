// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package common

import chisel3._
import chisel3.simulator.ChiselSim
import org.scalatest.flatspec.AnyFlatSpec
import Misc._
import ConvTestPats._

class V2FConvSpec extends AnyFlatSpec with ChiselSim {
  behavior of "Variable-Fix Converters"

  def checkInitConditionV2FConv(c: V2FConv): Unit = {
    // ChiselSim doesn't have initSource/initSink, just check initial state
    assert(c.io.in.ready.peek().litToBoolean, "in.ready should be true initially")
    assert(!c.io.out.valid.peek().litToBoolean, "out.valid should be false initially")
  }

  "V2FConv" should "pass" in {
    simulate(new V2FConv(vencbusbw, fdbusbw, packetbw, p_debuglevel = 1)) {
      c => {
        checkInitConditionV2FConv(c)

        def testpat(tp: List[Int]): Unit = {
          val expectedOutput = genoutputfromtestpat(tp)
          // ChiselSim doesn't support fork - rewrite as sequential
          c.io.out.ready.poke(true.B)
          var clk = 0
          var cnt = 1
          var inputIdx = 0
          
          // Process inputs and outputs sequentially
          while (inputIdx < 16 || c.io.out.valid.peek().litToBoolean) {
            // Enqueue next input if ready and available
            if (inputIdx < 16 && c.io.in.ready.peek().litToBoolean) {
              c.io.in.bits.poke(0x123456789L.S)
              c.io.in.valid.poke(true.B)
              c.clock.step()
              c.io.in.valid.poke(false.B)
              inputIdx += 1
            }
            
            // Dequeue output if valid
            if (c.io.out.valid.peek().litToBoolean && cnt <= 4) {
              val outbits = c.io.out.bits.peek().litValue.toInt
              println(f"clk${clk}/cnt${cnt}  ${outbits}%32x")
              cnt += 1
              c.clock.step()
              clk += 1
            } else {
              c.clock.step()
              clk += 1
            }
          }
        }
        testpat(testpat_l0)
      }
    }
  }
}




// ==============================================================
// The code below will be removed
// ==============================================================
class UnusedSpec extends AnyFlatSpec with ChiselSim {

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
    simulate(new ConvV2F(bw = packetbw, nblocks = nblocks)) { c =>
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
            val valid = c.io.out.valid.peek().litToBoolean
            if (valid) {
              val out = c.io.out.bits.peek().litValue.toInt
              val outref = castListInt2BigInt(ref, packetbw)
              println(f"${out.toHexString} ${outref.toString(16)} ${out == outref}")
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
        val valid = c.io.out.valid.peek().litToBoolean
        if (valid) {
          val out = c.io.out.bits.peek().litValue.toInt
          println(f"${out.toHexString}")
        }
      }
      //refs.foreach { r => testLoopWithExpectedValues(r) }
      testLoopWithExpectedValues(refs(3))
    }
  }

  "ConvV2F init condition" should "pass" in {
    simulate(new ConvV2F(bw = packetbw, nblocks = nblocks)) { c =>
      // ChiselSim doesn't have initSource/initSink, just check initial state
      assert(c.io.in.ready.peek().litToBoolean, "in.ready should be true initially")
      assert(!c.io.out.valid.peek().litToBoolean, "out.valid should be false initially")

      // ChiselSim doesn't support fork - rewrite as sequential
      c.io.out.ready.poke(true.B)
      // fill all the blocks to generate an output
      for (_ <- 1 to nblocks) {
        while (!c.io.in.ready.peek().litToBoolean) c.clock.step()
        c.io.in.bits.len.poke(1)
        for (j <- 0 until nblocks) {
          if (j == 0) c.io.in.bits.data(j).poke(1)
          else c.io.in.bits.data(j).poke(0)
        }
        c.io.in.valid.poke(true.B)
        c.clock.step()
      }
      // Wait for output
      while (!c.io.out.valid.peek().litToBoolean) c.clock.step()
      c.io.out.bits.expect(castListInt2BigInt(List.fill(nblocks){1}, packetbw).U)
    }
  }
}
