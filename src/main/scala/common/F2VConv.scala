// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package common

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._


/**
 * This module generate a circuit that receives a fixed size buffer, extract variable-length data and outputs it.
 *
 * @param inbw   the bit width of the input data
 * @param outbw  the bit width of the output data
 * @param packetbw the bit width of each packet
 *
 * Note: This does not match with ConvV2F
 */
class F2VConv(inbw: Int = 128, outbw: Int = 36, packetbw: Int = 4, debuglevel: Int = 0) extends Module {
  override def desiredName = s"F2VConv_inbw${inbw}_outbw${outbw}_packetbw$packetbw"

  require((inbw % packetbw) == 0)
  require(Utils.isPowOfTwo(inbw))

  val ninpackets = inbw / packetbw
  val noutpackets = outbw / packetbw // does not include header
  require(ninpackets > (noutpackets*3))  // *3 makes enough space

  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(UInt(inbw.W)))
    val out = Decoupled(SInt(outbw.W))
  })

  val inBufReg = RegInit(VecInit(Seq.fill(ninpackets)(0.U(packetbw.W)))) // a copy of input
  val inBufInProcessReg = RegInit(false.B)
  val headerPosReg = RegInit(0.U(log2Ceil(ninpackets).W)) // point the next header position

  // two stages
  // stage1 is copied to stage2 when a new data comes
  // the partials get merged at the stage2.
  // stage1 regs that are shifted to stage2
  val headerNeg1Reg = RegInit(false.B) // true if negative
  val outBuf1Reg = RegInit(VecInit(Seq.fill(noutpackets)(0.U(packetbw.W))))
  val outBufReady1Reg = RegInit(false.B) // becomes true when fully populated
  // stage1 regs that are used in stage2
  val headerLen1Reg = RegInit(0.U(log2Ceil(noutpackets).W)) // the number of packets
  val isOutBufPartial1Reg = RegInit(false.B) // only to stage2
  val outBufPartialLen1Reg = RegInit(0.U(log2Ceil(noutpackets).W)) // only to stage2
  // stage2
  val headerNeg2Reg = RegInit(false.B) // true if negative
  val outBuf2Reg = RegInit(VecInit(Seq.fill(noutpackets)(0.U(packetbw.W))))
  val outBufReady2Reg = RegInit(false.B) // becomes true when fully populated

  // if outBufReady2Reg is ready, it will be sent to the output
  io.out.valid := outBufReady2Reg
  io.out.bits := Mux(headerNeg2Reg,
    -1.S * Cat(outBuf2Reg.reverse).asSInt,
    Cat(outBuf2Reg.reverse).asSInt)

  when (io.out.valid && io.out.ready) {
    outBufReady2Reg := false.B // assume that out is consumed
  }

  io.in.ready := !inBufInProcessReg

  // there is a case that no more incoming data
  when(!io.in.valid && !inBufInProcessReg) {
    outBufReady1Reg := false.B // no new data
    // shift stage1 to stage2
    outBufReady2Reg := outBufReady1Reg
    headerNeg2Reg := headerNeg1Reg
    for (i <- 0 until noutpackets) outBuf2Reg(i) := outBuf1Reg(i)
  }

  // copy io.in into bufReg, handle the last-half partial (stage2) and a full data (stage1)
  when(io.in.valid && !inBufInProcessReg) {
    // cast UInt to Vec to reduce the shift granularity
    val buftmp = Wire(Vec(ninpackets, UInt(packetbw.W)))
    for (i <- 0 until ninpackets) {
      buftmp(i) := io.in.bits((i + 1) * packetbw - 1, i * packetbw)
      inBufReg(i) := buftmp(i)
    }
    inBufInProcessReg := true.B // even after the maximum possible combination of partial and full data. still true

    // ==== to the second stage ===
    // partial is handled when the new input arrives.
    when(isOutBufPartial1Reg) { // the rest of the partial always starts at position 0
      val rest = Wire(Vec(noutpackets, UInt(packetbw.W)))
      // XXX: optimize this shift logic
      for (i <- 0 until noutpackets) {
        rest(i) := Mux(i.U<outBufPartialLen1Reg,
          0.U,
          Mux(i.U < headerLen1Reg,
            buftmp(i.U - outBufPartialLen1Reg),
            0.U)
        )
      }
      // merge into stage2 and reset stage1 regs
      if (debuglevel>0) printf("f2v: outBufPartialLen1Reg=%d\n", outBufPartialLen1Reg)
      for (i <- 0 until noutpackets) { // fixed size relaxes the resource
        outBuf2Reg(i) := Mux(i.U < outBufPartialLen1Reg,
          outBuf1Reg(i), rest(i)
        )
        outBuf1Reg(i) := 0.U // clear stage1 buf
        //printf("inserting %d %d %d\n", i.U, rest(i), i.U < outBufPartialLen1Reg)
      }
      outBufReady2Reg := true.B
      val nextpos = headerLen1Reg - outBufPartialLen1Reg
      headerPosReg := nextpos
      if (debuglevel>0) printf("f2v: partialhandling: nextpos=%d\n", nextpos)
    }.otherwise {
      // the previous full data will be shifted to the second stage
      for (i <- 0 until noutpackets) {
        outBuf2Reg(i) := outBuf1Reg(i)
        outBuf1Reg(i) := 0.U // clear stage1 buf
      }
      outBufReady2Reg := outBufReady1Reg
      headerPosReg := 0.U // since no partial, the next header should be at 0
      if (debuglevel>0) printf("f2v: fullhandling: nextpos=0\n")
    }
    headerNeg2Reg := headerNeg1Reg // regardless of full or partial

    // reset stage1 regs since no data is detected here
    isOutBufPartial1Reg := false.B
    headerNeg1Reg := false.B
    outBufReady1Reg := false.B
  }

  // after io.in is copied to inBufReg.  set inBufInProcessReg false when no longer data to be processed
  when(inBufInProcessReg) {
    // shift stage1 to stage2
    //printf("shifting to stage2: ")
    for (i <- 0 until noutpackets) {
      outBuf2Reg(i) := outBuf1Reg(i)
      //printf("%x", outBuf1Reg(i))
    }
    //printf("\n")
    headerNeg2Reg := headerNeg1Reg // regardless of full or partial
    outBufReady2Reg := outBufReady1Reg

    // stage1
    val tmpheader = Wire(UInt(packetbw.W))
    tmpheader := inBufReg(headerPosReg) // XXX: might be costly. optimize this later.
    // ========= header decoding: this should be implemented externally
    val hdec = Module(new HeaderDecode4b(noutpackets))
    hdec.io.in := tmpheader
    val npayloads = Wire(UInt(noutpackets.W))
    npayloads := hdec.io.outlen
    headerLen1Reg := npayloads
    // =========
    if (debuglevel > 0) printf("f2v: inprocessing: npayloads=%d headerpos=%d\n",npayloads, headerPosReg)
    // stage1: regular packet or first half partial

    val hpos = Cat(0.U(1.W), headerPosReg) // need to extend 1 bit, otherwise overflow
    when(npayloads===0.U) {
      for (i <- 0 until noutpackets) outBuf1Reg(i) := 0.U
    }.otherwise {
      for (i <- 0 until noutpackets) {
        val idx = hpos + (i + 1).U
        val idxend = hpos + npayloads + 1.U
        outBuf1Reg(i) := Mux(idx < idxend,
          inBufReg(idx),
          0.U)
      }
    }
    outBufReady1Reg := true.B

    val newhpos = Wire(UInt(log2Ceil(ninpackets).W))
    newhpos := headerPosReg + npayloads + 1.U // +1.U because of header
    when(newhpos < headerPosReg) { // moved to the next input
      inBufInProcessReg := false.B
      if (debuglevel>0) printf("f2v: move to the next input\n")
    }
    headerPosReg := newhpos
    if (debuglevel>0) printf("f2v: inprocessing: newhpos=%d\n", newhpos)

    val nremains = ninpackets.U - headerPosReg  // space including header
    if (debuglevel>0) printf("f2v: inprocessing: npayloads=%d nremains=%d\n", npayloads, nremains)
    when(npayloads + 1.U <= nremains) { // regular packets
      isOutBufPartial1Reg := false.B
      outBufPartialLen1Reg := 0.U
      if (debuglevel>0) printf("f2v: inprocessing: next full\n")
    }.otherwise {
      isOutBufPartial1Reg := true.B
      outBufPartialLen1Reg := nremains - 1.U // how many payloads get copied
      if (debuglevel>0) printf("f2v: inprocessing: next partial\n")
    }
  }
}

object F2VConvDriver {
  def main(args: Array[String]): Unit = {
    (new ChiselStage).emitVerilog(new F2VConv())
  }
}
