// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>
//
// the idea is from T. Ueno et al.,"Bandwidth Compression of Floating-Point Numerical Data Streams for FPGA-based High-Performance Computing""

package common

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._
//import _root_.circt.stage.ChiselStage

// v>=0: flip the sign bit
// v <0: flip all bits

class MapFP2UInt(bw: Int = 32) extends Module {
  require(bw == 32 || bw == 64)

  override def desiredName = s"MapFP2UInt$bw"

  val io = IO(new Bundle {
    val in = Input(UInt(bw.W))
    val rev = Input(Bool())
    val out = Output(UInt(bw.W))
  })
  io.out := Mux(
    io.rev,
    Mux(io.in(bw - 1), Cat(~io.in(bw - 1), io.in(bw - 2, 0)), ~io.in),
    Mux(io.in(bw - 1), ~io.in, Cat(~io.in(bw - 1), io.in(bw - 2, 0)))
  )
}

class MapFP2UIntIdentity(bw: Int = 32) extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(bw.W))
  })

  val fp2uint = Module(new MapFP2UInt(bw))
  val revfp2uint = Module(new MapFP2UInt(bw))

  fp2uint.io.in := io.in
  fp2uint.io.rev := false.B
  revfp2uint.io.in := fp2uint.io.out
  revfp2uint.io.rev := true.B

  assert(revfp2uint.io.out === io.in)
}

object IntegerizeFPDriver extends App {
  (new ChiselStage).emitVerilog(new MapFP2UInt())
  // ChiselStage.emitSystemVerilog(new MapFP2UInt()) // 5.0.0
}


object IntegerizeFPSpecUtil {
  import java.nio.ByteBuffer

  val bit31 = BigInt(1) << 31
  val bit32 = BigInt(1) << 32
  val mask31 = bit31 - 1
  val mask32 = bit32 - 1

  def ifp32Forward(v: BigInt): BigInt = {
    val sign = (v & bit31) > 0
    if (sign) v ^ mask32
    else bit31 | v
  }

  def ifp32Backward(v: BigInt): BigInt = {
    val sign = (v & bit31) > 0
    if (sign) v & mask31
    else v ^ mask32
  }

  def convFloat2Bin(f: Float): BigInt = {
    val buf = ByteBuffer.allocate(4)
    buf.putFloat(f)
    buf.flip()
    val intbits = buf.getInt
    BigInt(intbits.toLong & 0xffffffffL) // prevent sign exntension
  }
  def convBin2Float(d: BigInt): Float = {
    val buf = ByteBuffer.allocate(4)
    val tmp = d.toByteArray

    if (tmp.length <= 4) {
      buf.put(Array.fill(4 - tmp.length)(0.toByte))
      buf.put(tmp)
    } else {
      buf.put(tmp.takeRight(4))
    }
    buf.flip()
    buf.getFloat
  }

  def testmain(args: Array[String]): Unit = {
    for (i <- List.tabulate(10) { i => -1.0f * i.toFloat / 10.0f }) {
      val a: BigInt = convFloat2Bin(i)
      val b: Float = convBin2Float(a)
      print(f"$i $a%8x $b : ")
      if (i != b) println("wrong")
      else println("correct")
    }
  }
}
