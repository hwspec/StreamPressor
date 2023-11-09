// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package common

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._
//import _root_.circt.stage.ChiselStage // for 5.0.0

// Counting leading zeros for two-bit unsigned integer
/*
class Clz2() extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(2.W))
    val out = Output(UInt(2.W))
  })

  io.out := 0.U

  switch(io.in) {
    is("b00".U) { io.out := "b10".U }
    is("b01".U) { io.out := "b01".U }
    is("b10".U) { io.out := "b00".U }
    is("b11".U) { io.out := "b00".U }
  }
}
 */

class ClzParam(val nb: Int = 16) extends Module {

  def ispow2(x: Int): Boolean = (x != 0) && (x & (x - 1)) == 0

  assert((nb >= 2) && ispow2(nb))

  override def desiredName = s"Clz$nb"

  val lognb = log2Ceil(nb)
  val out_nbits = lognb + 1

  val io = IO(new Bundle {
    val in = Input(UInt(nb.W))
    // the number of the leading zeros (Nlz) of the input data, which
    // requires (log2Ceil(nb)+1) bits to hold the number. e.g., a
    // 16-bit input, the number of the leading-zeros is between 0 and
    // 16 (all bits are zeros), which requires 5 bits.
    val out = Output(UInt(out_nbits.W))
  })

  // When nb is 2, do a simple lookup. The input "00" has two leading
  // zero, the input "01" has one leading zero, and the rest has no
  // leading zero.
  if (nb == 2) io.out := MuxLookup(io.in, 0.U, Array(0.U -> 2.U, 1.U -> 1.U).toIndexedSeq)
  else {
    // divide 'in' into two blocks: c0 (lower half) and c1 (upper half).
    val half = nb >> 1
    val c0 = Module(new ClzParam(half))
    val c1 = Module(new ClzParam(half))
    c0.io.in := io.in(half - 1, 0)
    c1.io.in := io.in(nb - 1, half)

    io.out := 0.U((lognb + 1).W)

    // If both two divided blocks only contain zero or MSB of the
    // counting number of both blocks are one, the original input 'in'
    // should only contain zero.
    when(c1.io.out(lognb - 1) && c0.io.out(lognb - 1)) {
      // All-zero case. simple set MSB of io.out one.
      io.out := Cat("b1".U, 0.U(lognb.W))
    }.elsewhen(c1.io.out(lognb - 1) === 0.U) {
      // the upper half contains one or more 1, so 'io.out' is simply
      // Nlz of the upper half, regardless of the content of the lower
      // half.
      io.out := Cat("b0".U, c1.io.out)
    }.elsewhen(c1.io.out(lognb - 1) === 1.U) {
      // the upper half only contain zero while the lower half is not
      // all-zero. 'io.out' is the upper half Nlz plus the lower half
      // Nlz.
      io.out := Cat("b01".U, c0.io.out(lognb - 2, 0))
    }
  }
}

object ClzParam {
  def main(args: Array[String]): Unit = {
    (new ChiselStage).emitVerilog(new ClzParam(32))
    /*
  ChiselStage.emitSystemVerilog(new ClzParam(32)
    , firtoolOpts = Array(
        "--disable-all-randomization",
        "--strip-debug-info",
        "--split-verilog",
        "-o=generated")
  )
     */
  }
}
