// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package common

object Misc {
  def biginthexstr(v: BigInt, nbits: Int = 64): String = {
    val n4b = nbits / 4
    val hstr = v.toString(16)
    val hstr2 = "0" * (n4b - hstr.length) + hstr
    hstr2
  }

  def castListInt2BigInt(a: List[Int], shiftbw: Int): BigInt = {
    val tmp = a.reverse.zipWithIndex.map {
      case (v, pos) => BigInt(v) << (pos * shiftbw)
    }
    tmp.sum
  }
}
