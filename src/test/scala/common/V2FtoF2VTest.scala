// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package common

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._

/**
 * This module is a testing purpose only.
 *
 * @param p_encbusbw the bus bitwidth for encoding data (SInt)
 * @param p_fixbusbw the output bus bitwidth
 * @param p_packetbw the bitwdith of each packet
 * @param p_debuglevel debug level (0: no message)
 */
class V2FtoF2VTest(p_encbusbw: Int = 36, p_fixbusbw: Int = 128, p_packetbw: Int = 4, p_debuglevel:Int = 0) extends Module {

  val io = IO(new Bundle {
    val inflush = Input(Bool()) // when it becomes true, the current buf is dumped
    val in  = Flipped(Decoupled(SInt(p_encbusbw.W)))
    val out = Decoupled(SInt(p_encbusbw.W))
  })

  val v2f = Module(new V2FConv(p_encbusbw, p_fixbusbw, p_packetbw, p_debuglevel))
  val f2v = Module(new F2VConv(p_fixbusbw, p_encbusbw, p_packetbw, p_debuglevel))

  v2f.io.inflush := io.inflush

  v2f.io.in <> io.in
  f2v.io.in <> v2f.io.out
  io.out    <> f2v.io.out
}

object V2FtoF2VDriver {
  def main(args: Array[String]): Unit = {
    (new ChiselStage).emitVerilog(new V2FtoF2VTest())
  }
}
