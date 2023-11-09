// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package common

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random
import scala.collection.mutable.ListBuffer

import Misc._

class VFConvSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Variable-fix Converters"

  val rnd = new Random()

  val packetbw = 4   // packet bitwidth, hold a header or payload
  val fdbusbw = 128  // bus bitwidth for fixed-size data (fd)
  val vencbusbw = 36     // bus bitwidth for variable-size encoding data (vd)
  val npacketsfdbus = fdbusbw / packetbw

  // XXX: sort of the following variables
  val nblocks = (1 << packetbw) // the maximum number of blocks
  val out_nbits = packetbw * nblocks // the size of the output bus

  val ntestpatlen = npacketsfdbus*1 + 3
  // test patterns, a list of headers that include only 'length' of each payload
  // length=0 means literal 0
  val testpat_l0 = List.fill(ntestpatlen)(0) // all elements are length 0
  val testpat_l1 = List.fill(ntestpatlen)(1) // all elements are length 1
  val testpat_l2 = List.fill(ntestpatlen)(2)
  val testpat_l3 = List.fill(ntestpatlen)(3)
  val testpat_seq = List.tabulate(ntestpatlen)(i => i % (1<<(packetbw-1))) // len code: 0 to 7
  val testpat_rnd = List.tabulate(ntestpatlen)(_ => rnd.nextInt(1<<(packetbw-1))) //

  val testpatterns = List(testpat_l0, testpat_l1, testpat_l2, testpat_l3, testpat_seq, testpat_rnd)

  def lutLen(n : Int): Int = if (n==7) 9 else n  // 4b header coding
  //def lutLen(n : Int): Int = if (n==7) 8 else n  // 4b header coding  // temporary until bug is fixed
  // should match with

  def calcnpacketsforpat(tp: List[Int]): Int = tp.map{v => lutLen(v) + 1}.reduce(_ + _)

  // MSB of each packet is always low to avoid treated as negative
  def genpayloadspat(n: Int): List[Int] = List.tabulate(n) {i => (i%((1<<(packetbw-1))-1))+1}

  def genpayloadval(n: Int): BigInt = {
    var plv = BigInt(0)
    val len = lutLen(n)
    for ((v,i) <- genpayloadspat(len).zipWithIndex) {
      plv = plv | (BigInt(v) << (i*packetbw))
    }
    plv
  }

  //
  // Verify V2FConv
  //

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
            c.io.in.enqueue(0x1234567.S)
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
  // ==============================================================
  def genfixedfrompat(pat: List[Int]): List[BigInt] = {
    val outbufs = ListBuffer[BigInt]()
    var pos : Int = 0
    var buf : BigInt = BigInt(0)

    pat.foreach { v =>
      // fill even overflow
      val header = v | 8 // MSB is negated sign bit (8 means the payload is positive)
      buf = buf | (BigInt(header) << (pos*packetbw))
      val len = lutLen(v)
      for ((v,i) <- genpayloadspat(len).zipWithIndex) {
        buf = buf | (BigInt(v) << ((pos+i+1)*packetbw)) // payload. note or
      }

      pos = pos+len+1 // the next header position
      if (pos >= npacketsfdbus) {
        val newbuf = buf & ((BigInt(1)<<fdbusbw)-1)
        outbufs += newbuf
        buf = buf >> fdbusbw
        pos = pos % npacketsfdbus
      }
    }
    if (pos > 0) {
      val newbuf = buf & ((BigInt(1)<<fdbusbw)-1)
      outbufs += newbuf
    }
    outbufs.toList
  }

  "Test genfixedfrompat" should "pass" in {
    def testrepreatpat(tp: List[Int]): Unit = {
      val res: List[BigInt] = genfixedfrompat(tp)
      val n = fdbusbw/packetbw
      val m = calcnpacketsforpat(tp)
      assert(res.length == (m + n - 1) / n)  // check the number of the outputs
      // for (elem <- res)   println(f"out${m}: ${biginthexstr(elem, fdbusbw).reverse}")
    }
    for (tp <- testpatterns) testrepreatpat(tp)
  }

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
