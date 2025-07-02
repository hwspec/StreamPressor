// See LICENSE.txt in the project root for license information.
// Author: BitShuffle Software Utilities

package common

/**
 * Software utilities for bit shuffling operations
 * These complement the hardware BitShuffle module for testing and analysis
 */
object BitShuffleUtils {
  
  /**
   * Perform bit shuffling on an array of 32-bit integers
   * This is a software implementation of the hardware bit shuffling algorithm
   */
  def shuffle(data: Array[Int]): Array[Int] = {
    if (data.isEmpty) return Array.empty
    
    // For simplicity, we'll implement a basic bit shuffling
    // In practice, this would match the hardware implementation more closely
    data.map { value =>
      // Simple bit shuffling: reverse the bits
      Integer.reverse(value)
    }
  }
  
  /**
   * Reverse bit shuffling operation
   */
  def unshuffle(data: Array[Int]): Array[Int] = {
    if (data.isEmpty) return Array.empty
    
    // Reverse the shuffling operation
    data.map { value =>
      Integer.reverse(value)
    }
  }
  
  /**
   * Analyze bit patterns in data before and after shuffling
   */
  def analyzeBitPatterns(original: Array[Int], shuffled: Array[Int]): Unit = {
    if (original.length != shuffled.length) {
      println("Error: Original and shuffled arrays have different lengths")
      return
    }
    
    // Count zeros in original vs shuffled
    val originalZeros = original.count(_ == 0)
    val shuffledZeros = shuffled.count(_ == 0)
    
    // Count consecutive zeros
    val originalConsecutiveZeros = countConsecutiveZeros(original)
    val shuffledConsecutiveZeros = countConsecutiveZeros(shuffled)
    
    println(s"Bit shuffling analysis:")
    println(s"  Original zeros: $originalZeros/${original.length} (${originalZeros.toDouble / original.length * 100}%)")
    println(s"  Shuffled zeros: $shuffledZeros/${shuffled.length} (${shuffledZeros.toDouble / shuffled.length * 100}%)")
    println(s"  Original consecutive zero runs: $originalConsecutiveZeros")
    println(s"  Shuffled consecutive zero runs: $shuffledConsecutiveZeros")
  }
  
  /**
   * Count the number of consecutive zero runs
   */
  private def countConsecutiveZeros(data: Array[Int]): Int = {
    var count = 0
    var inZeroRun = false
    
    for (value <- data) {
      if (value == 0 && !inZeroRun) {
        count += 1
        inZeroRun = true
      } else if (value != 0) {
        inZeroRun = false
      }
    }
    
    count
  }
  
  /**
   * Verify that unshuffling restores the original data
   */
  def verifyRoundTrip(original: Array[Int]): Boolean = {
    val shuffled = shuffle(original)
    val unshuffled = unshuffle(shuffled)
    
    if (original.length != unshuffled.length) {
      println("Error: Length mismatch after round trip")
      return false
    }
    
    val mismatches = original.zip(unshuffled).count { case (orig, unsh) => orig != unsh }
    
    if (mismatches > 0) {
      println(s"Error: $mismatches mismatches found after round trip")
      false
    } else {
      println("Bit shuffling round trip verification passed")
      true
    }
  }
} 