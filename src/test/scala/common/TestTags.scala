// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package common

import org.scalatest.Tag

/**
 * Test tags for categorizing tests
 * 
 * RequiresVerilator: Tests that require Verilator and firtool to run
 * RequiresVcs: Tests that require VCS simulator
 */
object TestTags {
  object RequiresVerilator extends Tag("RequiresVerilator")
  object RequiresVcs extends Tag("RequiresVcs")
}

 