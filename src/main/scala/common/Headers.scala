// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package common

import chisel3._
import chisel3.util._

class HeaderDecode4b(maxlen: Int) extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(4.W))
    val outneg = Output(Bool())  // true if negative, false otherwise
    val outlen = Output(UInt(log2Ceil(maxlen).W))
  })
  io.outneg := Mux(io.in(3), false.B, true.B) // note: 1xxx is positive, 0xxxx is negative
  //io.outlen := Mux(io.in(2, 0) === 7.U, (maxlen-1).U, io.in(2, 0))  // XXX: (maxlen-1) is temporaly. sync with lutLen in Spec
  io.outlen := Mux(io.in(2, 0) === 7.U, maxlen.U, io.in(2, 0))
}
