// See LICENSE.txt in the project root for license information.
// Author: Kazutomo Yoshii <kazutomo@mcs.anl.gov>

package estimate

object LPECompEstimateCR {
  import common.IntegerizeFPSpecUtil._
  import lpe.LagrangePredSpecUtil._
  import lpe.LagrangePredUtil._  // outSIntBits
  // import java.io._
  import java.nio._
  import java.nio.file._

  // XXX: create a class with a class parameter for coefficient
  /*
    0: 1
    1: 2 - 1
    2: 3 - 3 + 1
    3: 4 - 6 + 4 - 1
   */
  val lagrangepred: List[Int] = List(4, -6, 4, -1)
  // val lagrangepred: List[Int] = List(3, -3, 1)

  val sint_nbits = outSIntBits(32, lagrangepred) // bit length required to a result form Lagrange coding
  def sint_size_aligned(v: Int, d:Int) : Int = if( (v%d)==0) v else ((v/d)+1)*d
  val sint_nbits_4b = sint_size_aligned(sint_nbits, 4)

  def readFile(fn: String): List[Float] = {
    val bytes = Files.readAllBytes(Paths.get(fn))
    val buf = ByteBuffer.wrap(bytes)
    buf.order(ByteOrder.LITTLE_ENDIAN)
    val n = bytes.length / 4
    val fparray = new Array[Float](n)
    var i = 0
    while (buf.hasRemaining && i < n) {
      fparray(i) = buf.getFloat
      i += 1
    }
    fparray.toList
  }


  // forward Lagrange encoding
  def forwardLP(data: List[Float]): List[BigInt] = {
    val databin: List[BigInt] = data
      .map(convFloat2Bin) // convert Float into binary using BigInt as storage
      .map(ifp32Forward) // IntegerizedFP

    performLagrangeForward(databin)
  }

  // backward Lagrange encoding
  def backwardLP(lpenc: List[BigInt]): List[Float] = {
    val lpdec: List[BigInt] = performLagrangeBackward(lpenc)
    lpdec
      .map(ifp32Backward)
      .map(convBin2Float)
  }

  // this function simply returns the minimum length of bits required to store v
  def idealBitLength(v: BigInt): Int = {
    if (v < 0) v.bitLength + 1 // bitLength does not include the sign bit
    else v.bitLength
  }

  //
  // coding below perform coding on every single input; no grouping
  //
  def align(n: Int, align: Int) : Int = {
    if (align<1) return 0
    val aligned_n = if ((n%align)>0) (n/align) + 1
    else n/align
    aligned_n * align
  }

  def code0BitLength(v: BigInt): Int = {
    val l = v.bitLength // unsigned len
    // assume 4-bit packing granularity
    // 1-bit for sign
    // 6-bit for length (up to 36 bits)
    align(1 + 6 + l, 1)
  }

  def code1BitLength(v: BigInt) : Int = {
    val l = v.bitLength // unsigned len
    // assume 3-bit packing granularity
    // 3-bit header. bit0=sign, bit12: 00 -> no data, 01 -> 4 bit, 10 -> 8 bit, 11 -> 36 bit
    val dsz = if (l == 0) 0 else if (l <=3 ) 3 else if (l <=9) 9 else sint_nbits_4b
    align(3 + dsz, 3)
  }

  def code2BitLength(v: BigInt): Int = {
    val l = v.bitLength // unsigned len
    // assume 2-bit packing granularity
    // 2-bit header. bit0=sign, bit1: 0 -> 4 bit, 1-> 36 bit
    val dsz = if (l <= 4) 4 else sint_nbits_4b
    align(2 + dsz, 2)
  }

  def code3BitLength(v: BigInt): Int = {
    val l = v.bitLength // unsigned len
    // assume 4-bit packing granularity
    // 4-bit header. bit3=~sign, bit2-0: 000 => lit0, 001 .. 111 (1-7
    // header: 0000 => distinguish from literal 0
    //         1000 => literal 0, no packet
    //         1110 => positive 6 packets
    //         1111 => positive full packets
    //         0001 => negative one packet
    //         0110 => nagative 6 packets
    //         0111 => negative full packets
    val dsz = if (v == 0) 0 // literal 0
    else if (l/4 < 7) align(l,4) // 1 to 6 packets follow (4 to 24 bits)
    else sint_nbits_4b  // 7: the entire length. 9 packets with 36 bits data

    val nbits = align(4 + dsz, 4)
    nbits
  }

  def checkCompAndDecomp(data: List[Float]): Boolean = {
    val lpenc = forwardLP(data)

    val TotalOrigBits = data.length * 32  // 32 bits
    val idealBits = lpenc.map(idealBitLength)
    val code0Bits = lpenc.map(code0BitLength)
    val code1Bits = lpenc.map(code1BitLength)
    val code2Bits = lpenc.map(code2BitLength)
    val code3Bits = lpenc.map(code3BitLength)

    println(s"Nbits: min=${idealBits.min} max=${idealBits.max}")
    println(s"Ideal CR: ${TotalOrigBits.toFloat / idealBits.sum.toFloat}")
    println(s"Code0 CR: ${TotalOrigBits.toFloat / code0Bits.sum.toFloat}")
    println(s"Code1 CR: ${TotalOrigBits.toFloat / code1Bits.sum.toFloat}")
    println(s"Code2 CR: ${TotalOrigBits.toFloat / code2Bits.sum.toFloat}")
    println(s"Code3 CR: ${TotalOrigBits.toFloat / code3Bits.sum.toFloat}")

    val recovered = backwardLP(lpenc)
    if (!data.sameElements(recovered)) { // sanity check
      println("Error: recovered data did not match with the original")
      false
    }
    true
  }

  def main(args: Array[String]): Unit = {
    val n = 1000
    val t1 = List.tabulate(n) { i => 1.0f + i.toFloat / n.toFloat } ::: List.fill(n/10)(0f)
    val t2 = List.tabulate(n) { i => math.sin(math.Pi * (i.toDouble/n.toDouble)).toFloat }  ::: List.fill(n/10)(0f)
    println(f"sint_nbits=${sint_nbits}")

    checkCompAndDecomp(t1)
    checkCompAndDecomp(t2)

    //val t3 = readFile("a.dat")
    //checkCompAndDecomp(t3)
  }
}
