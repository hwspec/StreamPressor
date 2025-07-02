// See LICENSE.txt in the project root for license information.
// Author: Conversion Utilities for StreamPressor

package common

/**
 * Software utilities for V2F (Variable to Fixed) and F2V (Fixed to Variable) conversions
 * These complement the hardware V2FConv and F2VConv modules for testing and analysis
 */
object ConversionUtils {
  
  /**
   * Simple V2F conversion - converts variable-length data to fixed-length blocks
   * This is a simplified version of the hardware V2FConv module
   */
  def v2fConvert(data: Array[Int], blockSize: Int = 128): Array[Float] = {
    if (data.isEmpty) return Array.empty
    
    // For simplicity, we'll convert each int to float
    // In practice, this would implement the full V2F algorithm
    data.map(_.toFloat)
  }
  
  /**
   * Simple F2V conversion - converts fixed-length blocks back to variable-length data
   * This is a simplified version of the hardware F2VConv module
   */
  def f2vConvert(data: Array[Float]): Array[Int] = {
    if (data.isEmpty) return Array.empty
    
    // For simplicity, we'll convert each float back to int
    // In practice, this would implement the full F2V algorithm
    data.map(_.toInt)
  }
  
  /**
   * Analyze conversion efficiency
   */
  def analyzeConversion(original: Array[Int], v2fConverted: Array[Float], f2vConverted: Array[Int]): Unit = {
    println(s"Conversion analysis:")
    println(s"  Original data length: ${original.length}")
    println(s"  V2F converted length: ${v2fConverted.length}")
    println(s"  F2V converted length: ${f2vConverted.length}")
    
    // Check for data loss
    if (original.length != f2vConverted.length) {
      println(s"  Warning: Length mismatch after round trip conversion")
    }
    
    // Check for precision loss
    val mismatches = original.zip(f2vConverted).count { case (orig, converted) => orig != converted }
    val precisionLoss = mismatches.toDouble / original.length * 100
    
    println(s"  Precision loss: ${precisionLoss}% ($mismatches mismatches)")
  }
  
  /**
   * Verify round trip conversion
   */
  def verifyRoundTrip(original: Array[Int]): Boolean = {
    val v2fConverted = v2fConvert(original)
    val f2vConverted = f2vConvert(v2fConverted)
    
    if (original.length != f2vConverted.length) {
      println("Error: Length mismatch after round trip conversion")
      return false
    }
    
    val mismatches = original.zip(f2vConverted).count { case (orig, converted) => orig != converted }
    
    if (mismatches > 0) {
      println(s"Error: $mismatches mismatches found after round trip conversion")
      false
    } else {
      println("Conversion round trip verification passed")
      true
    }
  }
} 