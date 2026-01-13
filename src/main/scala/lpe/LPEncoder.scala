// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>
//
// the encoder design is described in T. Ueno et al.,"Bandwidth Compression of Floating-Point Numerical Data Streams for FPGA-based High-Performance Computing""

package lpe

import chisel3._
import common.{ClzParam, MapFP2UInt}
import chisel3.util._

// XXX: change the name later
class LPEncoder(val bw: Int = 32, fpmode: Boolean = false, coefficients: Seq[Int] = Seq(4, -6, 4, -1)) extends Module {
  import lpe.LagrangePredUtil._
  val sint_bw =
    outSIntBits(
      bw,
      coefficients
    ) // the minimum bit-length of a signed integer that can hold the result from Lagrange pred.
  val uint_bw = sint_bw - 1
  // val bw_uint_bw = log2Ceil(uint_bw+1) // bit-length of an integer that can hold 0 to out_uint_bw

  val cs = coefficients.map { v => v.S(bw.W) }
  val n = cs.length
  override def desiredName = s"LPEncoder_bw${bw}_n$n"

  val io = IO(new Bundle {
    val in_data = Input(UInt(bw.W))
    //val in_last = Input(UInt(bw.W))
    val out_data = Output(UInt(uint_bw.W))
    val out_sign = Output(UInt(1.W))
  })

  val pred = Module(new LagrangePred(bw, coefficients))

  val converted = Wire(UInt(bw.W))
  if (fpmode) {
    val ifp = Module(new MapFP2UInt(bw)) // insert the fp to int converter
    ifp.io.in := io.in_data
    ifp.io.rev := false.B
    converted := ifp.io.out
  } else {
    converted := io.in_data
  }
  pred.io.in := converted
  val diff = Wire(SInt(sint_bw.W))
  diff := converted.zext - pred.io.out

  when(diff < 0.S) {
    val tmp = Wire(SInt())
    tmp := -1.S * diff
    io.out_data := tmp(sint_bw - 2, 0)
    io.out_sign := 1.U
  }.otherwise {
    io.out_data := diff(sint_bw - 2, 0).asUInt
    io.out_sign := 0.U
  }

  val bw_clz = 1 << (BigInt(
    sint_bw - 1
  ).bitLength + 1) // round up to a power of two number because ClzParam only supports numbers that are powers of two.
  val clz = Module(new ClzParam(bw_clz))
  clz.io.in := io.out_data
//  io.out_nbits := bw_clz.U - clz.io.out
}

class LPDecoder(val bw: Int = 32, fpmode: Boolean = false, coefficients: Seq[Int] = Seq(4, -6, 4, -1)) extends Module {

  import lpe.LagrangePredUtil._
  val sint_bw = outSIntBits(bw, coefficients)
  val uint_bw = sint_bw - 1

  val cs = coefficients.map { v => v.S(bw.W) }
  val n = cs.length
  override def desiredName = s"LPDecoder_bw${bw}_n$n"

  val io = IO(new Bundle {
    val in_data = Input(UInt(uint_bw.W))
    val in_sign = Input(UInt(1.W))
    val out = Output(UInt(bw.W))
  })

  val sint_data = Wire(SInt(sint_bw.W))
  when(io.in_sign === 0.U) {
    sint_data := Cat(0.U(1.W), io.in_data).zext
  }.otherwise {
    sint_data := -1.S * io.in_data
  }

  val pred = Module(new LagrangePred(bw, coefficients))

  val tmp = Wire(SInt(sint_bw.W))
  val recovered = Wire(UInt(bw.W))
  tmp := pred.io.out + sint_data
  recovered := tmp(bw - 1, 0).asUInt
  pred.io.in := recovered

  if (fpmode) {
    val ifp = Module(new MapFP2UInt(bw)) // insert the fp to int converter
    ifp.io.in := recovered
    ifp.io.rev := true.B
    io.out := ifp.io.out
  } else {
    io.out := recovered
  }
}

/**
 * LPCompIdentity check see if decode(encode(x)) == x
 *
 * @param bw the bitwidth of input data
 * @param fpmode enables IntegerizeFP
 * @param coefficients coefficients for the Lagrange prediction
 */
class LPCompIdentity(bw: Int = 32, fpmode: Boolean = false, coefficients: Seq[Int] = Seq(4, -6, 4, -1)) extends Module {
  import LagrangePredUtil._

  val out_sint_bw = outSIntBits(bw, coefficients)

  val in = IO(Input(UInt(bw.W)))
  val out = IO(Output(UInt(bw.W)))

  val lp_enc = Module(new LPEncoder(bw, fpmode, coefficients = coefficients))
  val lp_dec = Module(new LPDecoder(bw, fpmode, coefficients = coefficients))

  lp_enc.io.in_data := in // internally assigned to a register; no delay is needed from the caller side

  lp_dec.io.in_data := lp_enc.io.out_data
  lp_dec.io.in_sign := lp_enc.io.out_sign
  out := lp_dec.io.out
  assert(lp_dec.io.out === in, "in and out should match")
  // printf("%d => %d%d => %d\n", in, lp_enc.io.out_sign, lp_enc.io.out_data, out)
}
