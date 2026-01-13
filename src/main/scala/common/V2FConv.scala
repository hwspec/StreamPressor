// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package common

import chisel3._
import circt.stage.ChiselStage
import chisel3.util._

/**
 * Variable to Fixed size packer converter
 *
 * @param bw the bitwidth of each block
 * @param nblocks the number of blocks
 *
 * Current limitations (will be fixed):
 * - if the consumer does not raise ready before switch the bank, the output will be corrupted
 *
 * XXX: add the details of V2F algorithm here
 *
 * Note about variable naming
 * 'p_' prefix  : class parameter
 * 'c_' prefix  : config variable in Scala domain
 * 'Reg' suffix : register
 * Otherwise, wires
 */
class V2FConv(p_inbw: Int = 36, p_outbw: Int = 128, p_packetbw: Int = 4, p_debuglevel:Int = 0) extends Module {
  override def desiredName = s"V2FConv_inbw${p_inbw}_outbw${p_outbw}_packetbw$p_packetbw"

  require((p_outbw % p_packetbw) == 0)
  require((p_inbw % p_packetbw) == 0)
  require(Utils.isPowOfTwo(p_outbw))

  val c_nmaxpackets = p_inbw / p_packetbw
  val c_nblocks: Int = p_outbw / p_packetbw

  class V2FIO extends Bundle {
    val in = Flipped(Decoupled(SInt(p_inbw.W)))
    val inflush = Input(Bool()) // when it becomes true, the current buf is dumped
    val out = Decoupled(Output(Bits(p_outbw.W))) // full
  }

  val io = IO(new V2FIO)

  val outBufReg = RegInit(VecInit(Seq.fill(c_nblocks)(0.U(p_packetbw.W))))
  val outBufReadyReg = RegInit(false.B)

  val bufReg = RegInit(VecInit(Seq.fill(c_nblocks * 2)(0.U(p_packetbw.W)))) // double-sized buffer
  val posbw = log2Ceil(c_nblocks * 2) // the bitwidth for a position in the double buffer
  val posReg = RegInit(0.U(posbw.W)) // the position in the double bufffer for a new data
  val bankReg = RegInit(0.U(1.W)) // 0: (nblocks-1, 0) 1: (nblocks*2-1, nblocks)

  io.in.ready := true.B // always accept

  when(io.in.fire) {
    // === input sint into a vector of packets,  encoding specific
    // XXX generalize the header handling and possibly parameterize it (sign and length)
    val inabsval = Mux(io.in.bits < 0.S, -1.S * io.in.bits, io.in.bits)
    val indatauint = inabsval.asUInt // unsigned int part
    val insign = Mux(io.in.bits < 0.S, true.B, false.B)

    // count the number of packets that will be sent.
    val clzbw = 1 << log2Ceil(p_inbw) // the smallest power of two number that is larger than databw. e.g., 36 => 64
    val nbits_clzbw = log2Ceil(clzbw) + 1  // +1 ???
    val clzmod = Module(new ClzParam(clzbw))
    clzmod.io.in := indatauint
    val ndatabits = clzbw.U(nbits_clzbw.W) - clzmod.io.out // the number of residual bits
    val nexactpayloads = (ndatabits + (p_packetbw - 1).U) / p_packetbw.U  // XXX: get rid of division

    // ==== construct the 4b header
    val headercodedlen = Mux(nexactpayloads < 7.U, nexactpayloads, 7.U) // use only 3 bits. 111 means all payloads (e.g., 9)
    val newheader = Mux(insign, headercodedlen, 8.U | headercodedlen) //

    val innheaderpayloads = Mux(nexactpayloads < 7.U, nexactpayloads, c_nmaxpackets.U) + 1.U

    val inheaderpayloadvec = Wire(Vec(c_nmaxpackets + 1, Bits(p_packetbw.W)))
    inheaderpayloadvec(0) := newheader
    for (i <- 1 until c_nmaxpackets + 1) {
      inheaderpayloadvec(i) := indatauint(i * p_packetbw - 1, (i - 1) * p_packetbw)
    }
    if (p_debuglevel > 0)
      printf("v2f: ndatabits=%d nexactpayloads=%d header=%x\n", ndatabits, nexactpayloads, newheader)
    // =========

    val newpos = Wire(UInt(posbw.W))
    newpos := posReg + innheaderpayloads

    posReg := newpos // next position. wrap around

    // insert the header and payloads at posReg
    for (i <- 0 until c_nmaxpackets + 1) { // +1 for header
      val updatepos = posReg + i.U // will wrap around
      bufReg(updatepos) :=
        Mux(i.U < innheaderpayloads,
          inheaderpayloadvec(i), 0.U)
    }

    // raise outbuf_valid when the position moves to the next bank
    when((posReg < c_nblocks.U && newpos >= c_nblocks.U) // move to the 2nd
      || (newpos < posReg)) { // move to the 1st from the 2nd
      bankReg := !bankReg
      if (p_debuglevel > 0) {
        printf("v2f: ready posReg=%d newpos=%d %x\n", posReg, newpos, bufReg.asUInt)
      }
      outBufReadyReg := true.B
    }
  }
  if (p_debuglevel > 0) {
    printf("v2f: outBufReadyReg=%d io.out.ready=%d\n", outBufReadyReg, io.out.ready)
  }
  // Note: if the consumer does not raise ready before switch the bank, the output will be corrupted
  io.out.valid := outBufReadyReg
  when(io.out.valid && io.out.ready) {
    if (p_debuglevel > 0) printf("v2f: Reading bankReg=%d\n", bankReg)
    outBufReadyReg := false.B
  }

  val revbuf = Cat(bufReg)
  when(bankReg === 0.U) {
    io.out.bits := revbuf(p_outbw - 1, 0)
  }.otherwise {
    io.out.bits := revbuf(p_outbw*2 - 1, p_outbw)
  }
}

object V2FConvDriver {
  def main(args: Array[String]): Unit = {
    ChiselStage.emitSystemVerilog(new V2FConv())
  }
}
