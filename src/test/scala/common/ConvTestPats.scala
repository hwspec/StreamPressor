package common

import scala.collection.mutable.ListBuffer
import scala.util.Random
import org.scalatest.flatspec.AnyFlatSpec

/**
 * ConvTestPats includes parameters and pre-generated test patterns for V2FConvSpec and F2VConvSpec
 *
 */
object ConvTestPats {
  val rnd = new Random()

  // header specific definition: 4-bit header (sign + 3-bit length)
  def lutLen(n: Int): Int = if (n == 7) 9 else n // 4b header coding

  def posHeader(v: Int): Int = v | 8 // MSB is negated sign bit (8 means the payload is positive)

  // below are parameters for spec
  val packetbw = 4 // packet bitwidth, hold a header or payload
  val fdbusbw = 128 // bus bitwidth for fixed-size data (fd)
  val vencbusbw = 36 // bus bitwidth for variable-size encoding data (vd)
  val npacketsfdbus = fdbusbw / packetbw

  // XXX: sort of the following variables
  val nblocks = (1 << packetbw) // the maximum number of blocks
  val out_nbits = packetbw * nblocks // the size of the output bus

  val ntestpatlen = npacketsfdbus * 1 + 3
  // test patterns, a list of headers that include only 'length' of each payload
  // length=0 means literal 0
  val testpat_l0 = List.fill(ntestpatlen)(0) // all elements are length 0
  val testpat_l1 = List.fill(ntestpatlen)(1) // all elements are length 1
  val testpat_l2 = List.fill(ntestpatlen)(2)
  val testpat_l3 = List.fill(ntestpatlen)(3)
  val testpat_seq = List.tabulate(ntestpatlen)(i => i % (1 << (packetbw - 1))) // len code: 0 to 7
  val testpat_rnd = List.tabulate(ntestpatlen)(_ => rnd.nextInt(1 << (packetbw - 1))) //

  val testpatterns = List(testpat_l0, testpat_l1, testpat_l2, testpat_l3, testpat_seq, testpat_rnd)

  def calcnpacketsforpat(tp: List[Int]): Int = tp.map { v => lutLen(v) + 1 }.reduce(_ + _)

  // MSB of each packet is always low to avoid treated as negative
  def genpayloadspat(n: Int): List[Int] = List.tabulate(n) { i => (i % ((1 << (packetbw - 1)) - 1)) + 1 }

  def genpayloadval(n: Int): BigInt = {
    var plv = BigInt(0)
    val len = lutLen(n)
    for ((v, i) <- genpayloadspat(len).zipWithIndex) {
      plv = plv | (BigInt(v) << (i * packetbw))
    }
    plv
  }

  def genfixedfrompat(pat: List[Int]): List[BigInt] = {
    val outbufs = ListBuffer[BigInt]()
    var pos: Int = 0
    var buf: BigInt = BigInt(0)

    pat.foreach { v =>
      // fill even overflow
      val header = posHeader(v)
      buf = buf | (BigInt(header) << (pos * packetbw))
      val len = lutLen(v)
      for ((v, i) <- genpayloadspat(len).zipWithIndex) {
        buf = buf | (BigInt(v) << ((pos + i + 1) * packetbw)) // payload. note or
      }

      pos = pos + len + 1 // the next header position
      if (pos >= npacketsfdbus) {
        val newbuf = buf & ((BigInt(1) << fdbusbw) - 1)
        outbufs += newbuf
        buf = buf >> fdbusbw
        pos = pos % npacketsfdbus
      }
    }
    if (pos > 0) {
      val newbuf = buf & ((BigInt(1) << fdbusbw) - 1)
      outbufs += newbuf
    }
    outbufs.toList
  }

}

class LocalTest extends AnyFlatSpec {
  import ConvTestPats._
  behavior of "ConvTestPats"
  "Test genfixedfrompat" should "pass" in {
    def testrepreatpat(tp: List[Int]): Unit = {
      val res: List[BigInt] = genfixedfrompat(tp)
      val n = fdbusbw / packetbw
      val m = calcnpacketsforpat(tp)
      assert(res.length == (m + n - 1) / n) // check the number of the outputs
      // for (elem <- res)   println(f"out${m}: ${biginthexstr(elem, fdbusbw).reverse}")
    }
    for (tp <- testpatterns) testrepreatpat(tp)
  }
}
