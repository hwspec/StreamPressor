// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package common

import chisel3._
import chisel3.util._  // Reverse(), Cat()

/**
 * Bitshuffling module
 *
 * @param p_innelems the number elems per channel
 * @param p_inbw the number of bits per elem
 */
class BitShuffle(p_innelems:Int = 16, p_inbw:Int = 8) extends Module {
  val io = IO(new Bundle {
    val in  = Input( Vec(p_innelems, UInt(p_inbw.W)))
    val out = Output(Vec(p_inbw, UInt(p_innelems.W)))
  })
  for (i <- 0 until p_inbw) {
    io.out(i) := Reverse(Cat(io.in.map(_(i))))
  }
}
