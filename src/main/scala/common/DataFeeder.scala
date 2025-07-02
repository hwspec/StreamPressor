// See LICENSE.txt in the project root for license information.
// Author: Data Feeder for StreamPressor

package common

import chisel3._
import chisel3.util._


/**
 * Data Feeder for streaming numpy data into compression pipeline
 * Converts float data to uint32 for bit shuffling
 */
class DataFeeder(
  val dataWidth: Int = 32,
  val bufferSize: Int = 1024
) extends Module {
  
  val io = IO(new Bundle {
    // Input control
    val start = Input(Bool())
    val filename = Input(UInt(256.W)) // Filename as uint (simplified for now)
    
    // Output data stream
    val data = Output(UInt(dataWidth.W))
    val valid = Output(Bool())
    val ready = Input(Bool())
    val last = Output(Bool())
    
    // Status
    val busy = Output(Bool())
    val done = Output(Bool())
    val error = Output(Bool())
    val elementCount = Output(UInt(32.W))
  })
  
  // States
  object State extends chisel3.ChiselEnum {
    val Idle, Loading, Streaming, Done, Error = Value
  }
  
  val state = RegInit(State.Idle)
  val dataBuffer = Reg(Vec(bufferSize, UInt(dataWidth.W)))
  val bufferIndex = RegInit(0.U(log2Ceil(bufferSize + 1).W))
  val totalElements = RegInit(0.U(32.W))
  val currentElement = RegInit(0.U(32.W))
  
  // Default outputs
  io.data := 0.U
  io.valid := false.B
  io.last := false.B
  io.busy := state =/= State.Idle
  io.done := state === State.Done
  io.error := state === State.Error
  io.elementCount := totalElements
  
  // State machine
  switch(state) {
    is(State.Idle) {
      when(io.start) {
        state := State.Loading
        currentElement := 0.U
        bufferIndex := 0.U
        // In real implementation, this would load from file
        // For now, we'll simulate with test data
      }
    }
    
    is(State.Loading) {
      // Simulate loading data into buffer
      // In real implementation, this would read from numpy file
      when(bufferIndex < bufferSize.U) {
        // Generate some test data (zeros and some non-zeros)
        val testValue = Mux(
          bufferIndex < 100.U,
          bufferIndex, // Some non-zero values
          0.U // Mostly zeros
        )
        dataBuffer(bufferIndex) := testValue
        bufferIndex := bufferIndex + 1.U
      }.otherwise {
        state := State.Streaming
        bufferIndex := 0.U
        totalElements := bufferSize.U
      }
    }
    
    is(State.Streaming) {
      when(io.ready && bufferIndex < totalElements) {
        io.valid := true.B
        io.data := dataBuffer(bufferIndex)
        io.last := (bufferIndex === totalElements - 1.U)
        
        when(io.last) {
          state := State.Done
        }.otherwise {
          bufferIndex := bufferIndex + 1.U
          currentElement := currentElement + 1.U
        }
      }
    }
    
    is(State.Done) {
      // Stay in done state until reset
    }
    
    is(State.Error) {
      // Stay in error state until reset
    }
  }
}

/**
 * Numpy Data Feeder - integrates with NumpyReaderScalaPy
 * This is a software component that can be used for testing
 */
object NumpyDataFeeder {
  
  /**
   * Feed numpy data as a stream of uint32 values
   */
  def feedNumpyData(filename: String, chunkSize: Int = 1024): Iterator[Int] = {
    NumpyReaderScalaPy.readNumpyData(filename) match {
      case scala.util.Success(data) =>
        println(s"Feeding ${data.length} elements from $filename")
        
        // Convert float to uint32 for bit shuffling
        data.iterator.map { floatVal =>
          // Convert float to uint32 using bit representation
          java.lang.Float.floatToRawIntBits(floatVal)
        }
        
      case scala.util.Failure(exception) =>
        println(s"Error reading numpy file: ${exception.getMessage}")
        Iterator.empty
    }
  }
  
  /**
   * Feed numpy data in chunks for processing
   */
  def feedNumpyDataInChunks(filename: String, chunkSize: Int = 1024): Iterator[Array[Int]] = {
    NumpyReaderScalaPy.readNumpyData(filename) match {
      case scala.util.Success(data) =>
        println(s"Feeding ${data.length} elements in chunks of $chunkSize")
        
        // Convert and chunk the data
        val uint32Data = data.map(java.lang.Float.floatToRawIntBits)
        uint32Data.grouped(chunkSize).map(_.toArray)
        
      case scala.util.Failure(exception) =>
        println(s"Error reading numpy file: ${exception.getMessage}")
        Iterator.empty
    }
  }
  
  /**
   * Get statistics about the data being fed
   */
  def getDataStats(filename: String): Option[(Int, Int, Double)] = {
    NumpyReaderScalaPy.getDataStats(filename) match {
      case scala.util.Success(stats) =>
        Some((stats.totalElements, stats.nonZeroCount, stats.zeroRatio))
        
      case scala.util.Failure(_) =>
        None
    }
  }
} 