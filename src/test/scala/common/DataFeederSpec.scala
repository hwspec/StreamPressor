// See LICENSE.txt in the project root for license information.
// Author: DataFeeder Test

package common

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DataFeederSpec extends AnyFlatSpec with Matchers {
  
  "NumpyDataFeeder" should "feed X-ray data correctly" in {
    val filename = "25-trimmed.npy"
    
    // Test feeding data as stream
    val dataStream = NumpyDataFeeder.feedNumpyData(filename)
    val firstFewValues = dataStream.take(20).toArray
    
    println(s"First 20 uint32 values from feeder:")
    firstFewValues.zipWithIndex.foreach { case (value, i) =>
      println(f"  [$i%2d]: 0x$value%08X (${java.lang.Float.intBitsToFloat(value)})")
    }
    
    // Verify we get the expected number of elements
    val stats = NumpyDataFeeder.getDataStats(filename)
    stats should not be empty
    
    val (totalElements, nonZeroCount, zeroRatio) = stats.get
    println(s"Data stats: total=$totalElements, nonZero=$nonZeroCount, zeroRatio=${zeroRatio * 100}%")
    
    totalElements shouldBe 32768
    nonZeroCount shouldBe 12371
    zeroRatio shouldBe 0.6225 +- 0.01
  }
  
  "NumpyDataFeeder" should "feed data in chunks" in {
    val filename = "25-trimmed.npy"
    val chunkSize = 1024
    
    val chunkIterator = NumpyDataFeeder.feedNumpyDataInChunks(filename, chunkSize)
    val chunks = chunkIterator.toArray
    
    println(s"Generated ${chunks.length} chunks of size $chunkSize")
    
    // Verify chunk sizes
    chunks.length shouldBe 32 // 32768 / 1024 = 32 chunks
    chunks.take(chunks.length - 1).foreach(_.length shouldBe chunkSize)
    chunks.last.length shouldBe 1024 // Last chunk should be full
    
    // Verify total elements
    val totalElements = chunks.map(_.length).sum
    totalElements shouldBe 32768
    
    // Check first chunk content
    val firstChunk = chunks.head
    println(s"First chunk (${firstChunk.length} elements):")
    firstChunk.take(10).zipWithIndex.foreach { case (value, i) =>
      val floatVal = java.lang.Float.intBitsToFloat(value)
      println(f"  [$i%2d]: 0x$value%08X ($floatVal)")
    }
  }
  
  "NumpyDataFeeder" should "convert float to uint32 correctly" in {
    val testFloats = Array(0.0f, 1.0f, -1.0f, 3.14f, Float.NaN, Float.PositiveInfinity)
    
    testFloats.foreach { floatVal =>
      val uint32Val = java.lang.Float.floatToRawIntBits(floatVal)
      val convertedBack = java.lang.Float.intBitsToFloat(uint32Val)
      
      println(f"Float: $floatVal -> UInt32: 0x$uint32Val%08X -> Float: $convertedBack")
      
      // NaN should convert back to NaN
      if (floatVal.isNaN) {
        convertedBack.isNaN shouldBe true
      } else {
        convertedBack shouldBe floatVal
      }
    }
  }
  
  "NumpyDataFeeder" should "handle zero patterns correctly" in {
    val filename = "25-trimmed.npy"
    
    val dataStream = NumpyDataFeeder.feedNumpyData(filename)
    val firstHundred = dataStream.take(100).toArray
    
    // Count zeros in first 100 elements
    val zeroCount = firstHundred.count(_ == 0)
    val zeroRatio = zeroCount.toDouble / firstHundred.length
    
    println(s"Zero ratio in first 100 elements: ${zeroRatio * 100}%")
    
    // Should have significant zeros (X-ray data characteristic)
    zeroRatio should be > 0.5
  }
} 