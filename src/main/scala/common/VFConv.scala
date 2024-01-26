// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package common

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._

//class InputDataLen(bw: Int, nblocks: Int) extends Bundle {
//  val data = Input(Vec(nblocks, UInt(bw.W)))
//  val len  = Input(UInt(log2Ceil(nblocks).W))  // indicates how many data blocks needs to be appended
//}

class InputDataLen(bw: Int, nblocks: Int) extends Bundle {
  val data = Vec(nblocks, UInt(bw.W))
  val len  = UInt(log2Ceil(nblocks).W)  // indicates how many data blocks needs to be appended
}

/**
 * Variable to Fixed size packer converter (ConvV2F is obsolete)
 *
 * @param bw the bitwidth of each block
 * @param nblocks the number of blocks
 *
 *
 * Current limitations (will be fixed):
 * - if the consumer does not raise ready before switch the bank, the output will be corrupted
  */
class ConvV2F(bw: Int = 4, nblocks: Int = 32) extends Module {
  override def desiredName = s"V2F_bw${bw}_nblocks$nblocks"

  class V2FIO extends Bundle {
    val in = Flipped(Decoupled(Input(new InputDataLen(bw, nblocks))))
    val out = Decoupled(Output(Bits((bw * nblocks).W)))  // full
  }

  val io = IO(new V2FIO)

  private val bufReg = RegInit(VecInit(Seq.fill(nblocks*2)(0.U(bw.W)))) // double-sized buffer
  private val buf = Cat(bufReg)
  private val posbw = log2Ceil(nblocks*2) // the bitwidth for a position in the double buffer
  private val posReg = RegInit(0.U(posbw.W))  // the position in the double bufffer for a new data
  private val bankReg = RegInit(0.U(1.W))  // 0: (nblocks-1, 0) 1: (nblocks*2-1, nblocks)
  private val outvalidReg = RegInit(false.B)
  //
  private val newpos = Wire(UInt(posbw.W))

  io.in.ready := true.B // for now, always accept

  newpos := Mux(io.in.valid, posReg + io.in.bits.len, posReg) // the new position after the data is appended

  when (io.in.valid) {
    posReg := newpos // next position. wrap around

    // raise outbuf_valid when the position moves to the next bank
    when( (posReg < nblocks.U && newpos >= nblocks.U) // move to the 2nd
      ||  (newpos < posReg)) { // move to the 1st from the 2nd
      bankReg := !bankReg
      outvalidReg := true.B
    }

    // append data into the double buffer
    for (i <- 0 until nblocks*2) {
      when(newpos > posReg) {
        when(i.U >= posReg && i.U <= newpos) {
          bufReg(i.U) := io.in.bits.data(i.U - posReg)
          //printf("a: %x ", io.in.data(i.U - posReg))
        }
        //when(i.U > newpos) {bufReg(i.U) := 0.U}  // filled zero for remaining
      }.otherwise { // wrap around
        when(i.U < newpos || i.U >= posReg) {
          bufReg(i.U) := io.in.bits.data(i.U - posReg)
          //printf("b: %x ", io.in.data(i.U - posReg))
        }
      }
    }
  }

  // Note: if the consumer does not raise ready before switch the bank, the output will be corrupted
  when (io.out.ready) {
    io.out.valid := outvalidReg
    when(outvalidReg === true.B) {
      outvalidReg := false.B
    }
  } .otherwise {
    io.out.valid := false.B
  }

  // fully-populated output
  when(bankReg === 0.U) {
    io.out.bits := buf(nblocks * bw - 1, 0)
  }.otherwise {
    io.out.bits := buf(nblocks * 2 * bw - 1, nblocks * bw)
  }
}

object ConvV2FDriver {
  def main(args: Array[String]): Unit = {
    val totalbits = 128
    val bw = 8

    (new ChiselStage).emitVerilog(new ConvV2F(bw = bw, nblocks = totalbits / bw))
  }
}
