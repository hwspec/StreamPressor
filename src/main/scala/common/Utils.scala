// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package common

object Utils {
  def isPowOfTwo(v: Int): Boolean = {
    v > 0 && (v & (v-1)) == 0
  }
}
