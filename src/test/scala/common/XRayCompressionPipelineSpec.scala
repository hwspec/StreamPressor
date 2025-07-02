// See LICENSE.txt in the project root for license information.
// Author: X-Ray Compression Pipeline Test

package common

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.util.{Try, Success, Failure}

/**
 * Test the complete X-ray compression pipeline:
 * .npy file → reader → feeder → bit shuffling → run-length encoding → V2F → F2V → output
 */
class XRayCompressionPipelineSpec extends AnyFlatSpec with Matchers {
  
  val filename = "test_data/25-trimmed.npy"
  
  "X-Ray Compression Pipeline" should "process data through complete pipeline" in {
    
    // Step 1: Read numpy file using ScalaPy
    println("Step 1: Reading numpy file...")
    val numpyData = NumpyReaderScalaPy.readNumpyData(filename)
    
    numpyData shouldBe a[scala.util.Success[_]]
    val data = numpyData.get
    println(s"Read ${data.length} elements from $filename")
    
    // Step 2: Convert to uint32 for bit shuffling
    println("Step 2: Converting to uint32...")
    val uint32Data = data.map(java.lang.Float.floatToRawIntBits)
    println(s"Converted ${uint32Data.length} elements to uint32")
    
    // Step 3: Apply bit shuffling
    println("Step 3: Applying bit shuffling...")
    val bitShuffled = BitShuffleUtils.shuffle(uint32Data)
    println(s"Bit shuffled ${bitShuffled.length} elements")
    
    // Step 4: Apply bit plane compression (length calculation + zero suppression)
    println("Step 4: Applying bit plane compression...")
    val (compressedData, metadata) = BitPlaneCompressor.compress(bitShuffled)
    println(s"Bit plane compressed to ${compressedData.length} bits")
    
    // Step 5: Convert to float (V2F)
    println("Step 5: Converting to float (V2F)...")
    val v2fConverted = ConversionUtils.v2fConvert(compressedData)
    println(s"V2F converted to ${v2fConverted.length} elements")
    
    // Step 6: Convert back to uint32 (F2V)
    println("Step 6: Converting back to uint32 (F2V)...")
    val f2vConverted = ConversionUtils.f2vConvert(v2fConverted)
    println(s"F2V converted to ${f2vConverted.length} elements")
    
    // Step 7: Decompress bit planes
    println("Step 7: Decompressing bit planes...")
    val decompressedData = BitPlaneCompressor.decompress(f2vConverted, metadata)
    println(s"Bit plane decompressed to ${decompressedData.length} elements")
    
    // Step 8: Reverse bit shuffling
    println("Step 8: Reversing bit shuffling...")
    val bitUnshuffled = BitShuffleUtils.unshuffle(decompressedData)
    println(s"Bit unshuffled to ${bitUnshuffled.length} elements")
    
    // Step 9: Convert back to float
    println("Step 9: Converting back to float...")
    val reconstructedData = bitUnshuffled.map(java.lang.Float.intBitsToFloat)
    println(s"Reconstructed ${reconstructedData.length} float elements")
    
    // Step 10: Verify reconstruction
    println("Step 10: Verifying reconstruction...")
    reconstructedData.length shouldBe data.length
    
    // Check that the data matches (allowing for floating point precision)
    val tolerance = 1e-6f
    val mismatches = data.zip(reconstructedData).count { case (original, reconstructed) =>
      math.abs(original - reconstructed) > tolerance
    }
    
    println(s"Found $mismatches mismatches out of ${data.length} elements")
    println(s"Reconstruction accuracy: ${((data.length - mismatches).toDouble / data.length) * 100}%")
    
    // The reconstruction should be very accurate
    mismatches shouldBe 0
    
    println("Pipeline test completed successfully!")
  }
  
  "X-Ray Compression Pipeline" should "handle data feeding correctly" in {
    
    // Test the data feeder integration
    println("Testing data feeder integration...")
    
    val stats = NumpyDataFeeder.getDataStats(filename)
    stats shouldBe defined
    
    val (totalElements, nonZeroCount, zeroRatio) = stats.get
    println(s"Data stats: $totalElements total, $nonZeroCount non-zero, ${zeroRatio * 100}% zeros")
    
    // Test chunked feeding
    val chunkSize = 1024
    val chunks = NumpyDataFeeder.feedNumpyDataInChunks(filename, chunkSize).toArray
    println(s"Generated ${chunks.length} chunks of size $chunkSize")
    
    // Verify chunk sizes
    chunks.foreach { chunk =>
      chunk.length should be <= chunkSize
    }
    
    // Test streaming feeding
    val streamData = NumpyDataFeeder.feedNumpyData(filename).toArray
    println(s"Streamed ${streamData.length} uint32 elements")
    
    streamData.length shouldBe totalElements
    
    println("Data feeder test completed successfully!")
  }
  
  "X-Ray Compression Pipeline" should "demonstrate compression effectiveness" in {
    
    println("Testing compression effectiveness...")
    
    // Read original data
    val originalData = NumpyReaderScalaPy.readNumpyData(filename).get
    val originalSize = originalData.length * 4 // 4 bytes per float
    
    // Apply compression pipeline with bit plane compression
    val uint32Data = originalData.map(java.lang.Float.floatToRawIntBits)
    val bitShuffled = BitShuffleUtils.shuffle(uint32Data)
    val (compressedData, metadata) = BitPlaneCompressor.compress(bitShuffled)
    
    // Calculate compression statistics
    val compressedSize = compressedData.length / 8 // Convert bits to bytes
    val stats = BitPlaneCompressor.calculateCompressionEffectiveness(originalSize, compressedSize, metadata)
    
    println(s"Original size: $originalSize bytes")
    println(s"Compressed size: $compressedSize bytes")
    println(f"Compression ratio: ${stats.compressionRatio}%.2fx")
    println(f"Space saved: ${stats.spaceSaved}%.1f%%")
    println(s"Zero bit planes eliminated: ${stats.zeroBitPlanesEliminated}/${stats.totalBitPlanes}")
    println(s"Non-zero bit planes: ${stats.nonZeroBitPlanes}")
    
    // Analyze bit plane sparsity
    val analysis = BitPlaneCompressor.analyzeBitPlaneSparsity(bitShuffled)
    println(f"Overall bit plane sparsity: ${analysis.overallSparsity * 100}%.1f%%")
    println(s"Total non-zero bits: ${analysis.totalNonZeroBits}/${analysis.totalBits}")
    
    // For X-ray data with high zero content, we should see good compression
    stats.compressionRatio should be > 1.0
    stats.spaceSaved should be > 0.0
    
    println("Compression effectiveness test completed!")
  }
} 