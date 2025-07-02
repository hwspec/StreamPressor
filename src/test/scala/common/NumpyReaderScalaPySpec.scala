// See LICENSE.txt in the project root for license information.
// Author: ScalaPy NumpyReader Test

package common

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NumpyReaderScalaPySpec extends AnyFlatSpec with Matchers {
  
  "NumpyReaderScalaPy" should "read X-ray data file correctly" in {
    val filename = "test_data/25-trimmed.npy"
    
    NumpyReaderScalaPy.readNumpyData(filename) match {
      case scala.util.Success(floatData) =>
        println(s"Successfully read numpy file: $filename")
        println(s"Total elements: ${floatData.length}")
        println(s"Value range: ${floatData.min} to ${floatData.max}")
        
        // Print values at the same indices as Python showed non-zero values
        println("Values at indices 25267-25277 (where Python showed non-zero):")
        for (i <- 25267 to 25277) {
          if (i < floatData.length) {
            println(s"  [$i]: ${floatData(i)}")
          }
        }
        
        // Print sample values
        println("Sample values (first 10):")
        floatData.take(10).zipWithIndex.foreach { case (value, index) =>
          println(s"  [$index]: $value")
        }
        
        println("Sample values (last 10):")
        floatData.takeRight(10).zipWithIndex.foreach { case (value, index) =>
          println(s"  [${floatData.length - 10 + index}]: $value")
        }
        
        // Basic validation
        floatData.length shouldBe 32768 // 2 * 128 * 128
        
        // Check for expected characteristics of X-ray data
        val nonZeroCount = floatData.count(_ != 0.0f)
        val zeroRatio = 1.0 - (nonZeroCount.toDouble / floatData.length)
        println(s"Zero ratio: ${zeroRatio * 100}%")
        println(s"Non-zero count: $nonZeroCount")
        
        // Should have significant zeros (X-ray data characteristic)
        zeroRatio should be > 0.5
        
        // Check value range
        val minVal = floatData.min
        val maxVal = floatData.max
        println(s"Value range: $minVal to $maxVal")
        
        // Should have reasonable values (not extremely small like the old reader)
        maxVal should be > 100.0f // Should have some significant values
        
        // Check for NaN values
        val nanCount = floatData.count(_.isNaN)
        println(s"NaN count: $nanCount")
        nanCount shouldBe 0
        
      case scala.util.Failure(exception) =>
        fail(s"Failed to read numpy file: ${exception.getMessage}")
    }
  }
  
  "NumpyReaderScalaPy" should "get data statistics correctly" in {
    val filename = "test_data/25-trimmed.npy"
    
    NumpyReaderScalaPy.getDataStats(filename) match {
      case scala.util.Success(stats) =>
        println(s"Data statistics:")
        println(s"  Total elements: ${stats.totalElements}")
        println(s"  Non-zero count: ${stats.nonZeroCount}")
        println(s"  Zero ratio: ${stats.zeroRatio * 100}%")
        
        stats.totalElements shouldBe 32768
        stats.zeroRatio should be > 0.5
        
      case scala.util.Failure(exception) =>
        fail(s"Failed to get data stats: ${exception.getMessage}")
    }
  }
  
  "NumpyReaderScalaPy" should "feed data in chunks correctly" in {
    val filename = "test_data/25-trimmed.npy"
    val chunkSize = 16
    
    val chunkIterator = NumpyReaderScalaPy.feedNumpyDataInChunks(filename, chunkSize)
    val firstChunk = chunkIterator.next()
    
    println(s"First chunk size: ${firstChunk.length}")
    println(s"First chunk values: ${firstChunk.mkString(", ")}")
    
    firstChunk.length shouldBe chunkSize
  }
} 