// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package common

import chisel3._
import chisel3.util._
import chiseltest._

import org.scalatest.flatspec.AnyFlatSpec

class BitShuffleSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("BitShuffle")

  val c_nelems: Int = 16
  val c_elemsize: Int = 8

  val pat0 = List.fill(c_nelems)(0)

  def genref(p: List[Int]) : List[Int] = {
    (0 until c_elemsize).map { sh =>
      p.zipWithIndex.map {case (v, i) =>
        ((v >> sh) & 1) << i}.reduce(_ | _)
    }.toList
  }

  "foo" should "pass" in {
    println(genref(pat0))
  }

  "Zero test" should "pass" in {
    test(new BitShuffle(c_nelems, c_elemsize)) {
      c => {
        val hpat = pat0.map(_.U)
        val ref = genref(pat0)
        for (i<-0 until c_nelems) c.io.in(i).poke(hpat(i))
        for (i<-0 until c_elemsize) c.io.out(i).expect(ref(i))
      }
    }
  }

  "Random test" should "pass" in {
    test(new BitShuffle(c_nelems, c_elemsize)) {

      c => {
        val n = c_nelems
        val b = c_elemsize
        val maxval = (1 << b) - 1

        val ntests = 10 // the number of tests
        val seed = 123
        val r = new scala.util.Random(seed)

        // def pickval(pos: Int) : Int =  pos%2
        def pickval(pos: Int): Int = {
          val v = r.nextInt(1000)
          val m = List.tabulate(5)(idx => idx * 100 + 100)
          if (v < m(0)) 0
          else if (v >= m(0) && v < m(1)) 1
          else if (v >= m(1) && v < m(2)) 2
          else if (v >= m(2) && v < m(3)) 3
          else if (v >= m(3) && v < m(4)) 4
          else r.nextInt(maxval + 1 - 5) + 5
        }

        // if the sh'th bit of v is 1, it returns 1, otherwise returns 0
        def bittest(v: Int, sh: Int): Int = if ((v & (1 << sh)) == 0) 0 else 1

        for (t <- 0 until ntests) {
          val data = List.tabulate(n)(i => pickval(i))
          val shuffled = genref(data)
//            List.tabulate(b) { bpos =>
//              List.tabulate(n) { idx => bittest(data(idx), bpos) << idx } reduce (_ | _)
//            }


          println("REF:")
          shuffled foreach { v => print(f"$v%04x ") }
          println()
          print("IN :")
          for (i <- 0 until n) {
            val tmp = data(i)
            print(f"$tmp%04x ")
            c.io.in(i).poke(tmp)
          }
          println()
          print("OUT:")
          for (j <- 0 until b) {
            val tmp = c.io.out(j).peekInt()
            print(f"$tmp%04x ")
            c.io.out(j).expect(shuffled(j))
            c.clock.step()
          }
          println()
        }
      }
    }
  }
}
