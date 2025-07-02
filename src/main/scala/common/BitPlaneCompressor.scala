// See LICENSE.txt in the project root for license information.
// Author: Bit Plane Compressor for StreamPressor

package common

/**
 * Bit Plane Compressor for bit-shuffled data
 * Implements length calculation and zero suppression for optimal compression
 */
object BitPlaneCompressor {
  
  /**
   * Compress bit-shuffled data using bit plane analysis
   * @param bitShuffledData Array of 32-bit integers after bit shuffling
   * @return (compressedData, metadata) where metadata contains length information
   */
  def compress(bitShuffledData: Array[Int]): (Array[Int], BitPlaneMetadata) = {
    if (bitShuffledData.isEmpty) {
      return (Array.empty, BitPlaneMetadata(0, Array.empty, 0))
    }
    
    val dataLength = bitShuffledData.length
    val numBitPlanes = 32 // 32-bit integers
    
    // Extract bit planes (each bit position across all values)
    val bitPlanes = extractBitPlanes(bitShuffledData)
    
    // Analyze which bit planes contain non-zero data
    val nonZeroBitPlanes = bitPlanes.zipWithIndex.filter { case (plane, _) =>
      plane.exists(_ != 0)
    }
    
    // Calculate compression metadata
    val length = nonZeroBitPlanes.length
    val zeroBitPlanes = numBitPlanes - length
    val compressionRatio = numBitPlanes.toDouble / length
    
    // Create metadata
    val metadata = BitPlaneMetadata(
      totalBitPlanes = numBitPlanes,
      nonZeroBitPlaneIndices = nonZeroBitPlanes.map(_._2).toArray,
      dataLength = dataLength
    )
    
    // Compressed data: only the non-zero bit planes
    val compressedData = nonZeroBitPlanes.flatMap(_._1)
    
    println(s"Bit plane compression analysis:")
    println(s"  Total bit planes: $numBitPlanes")
    println(s"  Non-zero bit planes: $length")
    println(s"  Zero bit planes eliminated: $zeroBitPlanes")
    println(f"  Compression ratio: ${compressionRatio}%.2fx")
    println(s"  Original size: ${dataLength * numBitPlanes} bits")
    println(s"  Compressed size: ${compressedData.length} bits")
    println(f"  Space saved: ${((1.0 - compressedData.length.toDouble / (dataLength * numBitPlanes)) * 100)}%.1f%%")
    
    (compressedData, metadata)
  }
  
  /**
   * Decompress bit plane compressed data
   * @param compressedData The compressed bit plane data
   * @param metadata The compression metadata
   * @return Original bit-shuffled data
   */
  def decompress(compressedData: Array[Int], metadata: BitPlaneMetadata): Array[Int] = {
    if (compressedData.isEmpty || metadata.totalBitPlanes == 0) {
      return Array.empty
    }
    
    val dataLength = metadata.dataLength
    val numBitPlanes = metadata.totalBitPlanes
    val nonZeroIndices = metadata.nonZeroBitPlaneIndices
    
    // Reconstruct all bit planes (zero and non-zero)
    val allBitPlanes = Array.fill(numBitPlanes)(Array.fill(dataLength)(0))
    
    // Fill in the non-zero bit planes
    var dataIndex = 0
    for (i <- nonZeroIndices.indices) {
      val bitPlaneIndex = nonZeroIndices(i)
      val bitPlaneData = compressedData.slice(dataIndex, dataIndex + dataLength)
      allBitPlanes(bitPlaneIndex) = bitPlaneData
      dataIndex += dataLength
    }
    
    // Reconstruct the original bit-shuffled data
    reconstructFromBitPlanes(allBitPlanes)
  }
  
  /**
   * Extract bit planes from bit-shuffled data
   * Each bit plane contains one bit position from all values
   */
  private def extractBitPlanes(data: Array[Int]): Array[Array[Int]] = {
    val numBitPlanes = 32
    val dataLength = data.length
    
    val bitPlanes = Array.fill(numBitPlanes)(Array.fill(dataLength)(0))
    
    for (bitPos <- 0 until numBitPlanes) {
      for (i <- data.indices) {
        bitPlanes(bitPos)(i) = (data(i) >> bitPos) & 1
      }
    }
    
    bitPlanes
  }
  
  /**
   * Reconstruct data from bit planes
   */
  private def reconstructFromBitPlanes(bitPlanes: Array[Array[Int]]): Array[Int] = {
    val dataLength = bitPlanes(0).length
    val reconstructed = Array.fill(dataLength)(0)
    
    for (i <- 0 until dataLength) {
      var value = 0
      for (bitPos <- bitPlanes.indices) {
        value |= (bitPlanes(bitPos)(i) << bitPos)
      }
      reconstructed(i) = value
    }
    
    reconstructed
  }
  
  /**
   * Analyze bit plane sparsity for X-ray data
   */
  def analyzeBitPlaneSparsity(bitShuffledData: Array[Int]): BitPlaneAnalysis = {
    val bitPlanes = extractBitPlanes(bitShuffledData)
    
    val sparsityByPlane = bitPlanes.map { plane =>
      val nonZeros = plane.count(_ != 0)
      val sparsity = 1.0 - (nonZeros.toDouble / plane.length)
      sparsity
    }
    
    val totalNonZeroBits = bitPlanes.map(_.count(_ != 0)).sum
    val totalBits = bitShuffledData.length * 32
    val overallSparsity = 1.0 - (totalNonZeroBits.toDouble / totalBits)
    
    BitPlaneAnalysis(
      sparsityByPlane = sparsityByPlane,
      overallSparsity = overallSparsity,
      totalNonZeroBits = totalNonZeroBits,
      totalBits = totalBits
    )
  }
  
  /**
   * Calculate compression effectiveness for X-ray data
   */
  def calculateCompressionEffectiveness(originalSize: Int, compressedSize: Int, metadata: BitPlaneMetadata): CompressionStats = {
    val compressionRatio = originalSize.toDouble / compressedSize
    val spaceSaved = (1.0 - compressedSize.toDouble / originalSize) * 100
    val zeroBitPlanesEliminated = metadata.totalBitPlanes - metadata.nonZeroBitPlaneIndices.length
    
    CompressionStats(
      compressionRatio = compressionRatio,
      spaceSaved = spaceSaved,
      zeroBitPlanesEliminated = zeroBitPlanesEliminated,
      nonZeroBitPlanes = metadata.nonZeroBitPlaneIndices.length,
      totalBitPlanes = metadata.totalBitPlanes
    )
  }
}

/**
 * Metadata for bit plane compression
 */
case class BitPlaneMetadata(
  totalBitPlanes: Int,
  nonZeroBitPlaneIndices: Array[Int],
  dataLength: Int
)

/**
 * Analysis results for bit plane sparsity
 */
case class BitPlaneAnalysis(
  sparsityByPlane: Array[Double],
  overallSparsity: Double,
  totalNonZeroBits: Int,
  totalBits: Int
)

/**
 * Compression statistics
 */
case class CompressionStats(
  compressionRatio: Double,
  spaceSaved: Double,
  zeroBitPlanesEliminated: Int,
  nonZeroBitPlanes: Int,
  totalBitPlanes: Int
) 