// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>
//
// Lagrange prediction is used in T. Ueno et al.,"Bandwidth Compression of Floating-Point Numerical Data Streams for FPGA-based High-Performance Computing""
//

package lpe

import chisel3._
//import chisel3.stage.ChiselStage
import chisel3.util._
// import _root_.circt.stage.ChiselStage

object LagrangePredUtil {
  def additionalBits(coefficients: Seq[Int]): Int = {
    def nbits(v: Int) = math.ceil(math.log(v + 1) / math.log(2)).toInt

    coefficients.map(v => nbits(math.abs(v))).max
  }

  def outUIntBits(bw: Int, coefficients: Seq[Int]): Int = {
    bw + additionalBits(coefficients)
  }

  def outSIntBits(bw: Int, coefficients: Seq[Int]): Int = {
    bw + additionalBits(coefficients) + 1
  }
}

/**
 * LagrangePred is a Langrange-based prediction logic
 *
 * @param bw           the bitwidth for input data
 * @param coefficients the filter coefficients in Seq[Int]
 *                     Below are coefficients for Lagrange prediction
 *                     0: 1
 *                     1: 2 - 1
 *                     2: 3 - 3 + 1
 *                     3: 4 - 6 + 4 - 1
 * @param out          predicated value
 */
class LagrangePred(val bw: Int = 32, coefficients: Seq[Int] = Seq(4, -6, 4, -1)) extends Module {

  import LagrangePredUtil._

  val cs = coefficients.map { v => v.S(bw.W) }
  val n = cs.length

  override def desiredName = s"LaGrangePred_bw${bw}_n$n"

  val out_sint_bw = outSIntBits(bw, coefficients)

  val io = IO(new Bundle {
    /** innput'in' is taken each cycle */
    val in = Input(UInt(bw.W))
    /** output 'out' is a predicted data, generated each cycle */
    val out = Output(SInt(out_sint_bw.W))
  })

  val regs = RegInit(VecInit(Seq.fill(n)(0.S(out_sint_bw.W))))

  regs(0) := Cat(0.U(1.W), io.in).zext // prevent from converting to negative values
  for (i <- 1 until n) regs(i) := regs(i - 1)
  io.out := VecInit.tabulate(n) { i => cs(i) * regs(i) }.reduce(_ + _)
}

class LagrangePredIdentity(bw: Int = 32, coefficients: Seq[Int] = Seq(4, -6, 4, -1)) extends Module {

  import LagrangePredUtil._

  val out_sint_bw = outSIntBits(bw, coefficients)

  val in = IO(Input(UInt(bw.W)))
  val out = IO(Output(UInt(bw.W)))

  val pred_enc = Module(new LagrangePred(bw, coefficients))
  val pred_dec = Module(new LagrangePred(bw, coefficients))

  pred_enc.io.in := in // internally assigned to a register; no delay is needed from the caller side
  val diff = Wire(SInt(out_sint_bw.W))
  diff := in.zext - pred_enc.io.out // prediction based on the previous N elements, where N is the length of coefficients

  val tmp = Wire(SInt(out_sint_bw.W))
  val recovered = Wire(UInt(bw.W))

  tmp := pred_dec.io.out + diff
  assert(tmp >= 0.S, "%b", tmp) // 1st identity: should be non-negative due to unsigned int input
  val maxval = 1.toLong << bw // note: bw should be less than 64
  assert(tmp < maxval.S, "%b", tmp) // 2nd identity: should be smaller than 1<<bw
  recovered := tmp(bw - 1, 0).asUInt
  assert(in === recovered, "%b === %b", in, recovered) // Finally, it should match with the original input
  pred_dec.io.in := recovered // for the next cycle
  out := recovered
}

object LagrangePredSpecUtil {
  def applyLagrange(inputs: List[BigInt], lagrangepred: List[Int] = List(4, -6, 4, -1)) : List[BigInt] = {
    val padding: List[BigInt] = List.fill(lagrangepred.length)(0)
    (padding:::inputs).sliding(lagrangepred.length)
      .map { w =>
        val z = w.zip(lagrangepred.reverse)
        z.foldLeft(BigInt(0)) { case (acc, (c, v)) => acc + c * v }
      }
      .toList
      .dropRight(1)
  }
  def performLagrangeForward(inputs: List[BigInt], lagrangepred: List[Int] = List(4, -6, 4, -1)) : List[BigInt] = {
    val pred = applyLagrange(inputs, lagrangepred)
    inputs.zip(pred).map { case (a,b) => a - b}
  }
  def performLagrangeBackward(diffs: List[BigInt], lagrangepred: List[Int] = List(4, -6, 4, -1)): List[BigInt] = {
    val padding: List[BigInt] = List.fill(lagrangepred.length)(0)
    val nlagragepred = lagrangepred.length

    def helper(diffs: List[BigInt], recovered: List[BigInt]) : List[BigInt] = {
      if (diffs.length == 0) recovered
      else {
        val ln = recovered.takeRight(nlagragepred)
        val r = ln.zip(lagrangepred.reverse).map { case(a, b) => a * b }.sum + diffs.head
        helper(diffs.tail, recovered ::: List(r))
      }
    }
    helper(diffs, padding).drop(nlagragepred)
  }

  def main(args: Array[String]): Unit = {
    val inputs: List[BigInt] =
      List[BigInt](10,20,30,40, 0,0,0,0,0,0,0,0)
    val diffList = performLagrangeForward(inputs)
    val recovered = performLagrangeBackward(diffList)
    println(diffList)
    println(recovered)
    println(f"validate: ${recovered.sameElements(inputs)}")
  }
}
