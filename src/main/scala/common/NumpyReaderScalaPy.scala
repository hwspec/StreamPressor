// See LICENSE.txt in the project root for license information.
// Author: ScalaPy-based Numpy Reader for StreamPressor

package common

import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.SeqConverters
import scala.util.{Try, Success, Failure}

/**
 * ScalaPy-based Numpy Reader
 * Uses Python's numpy directly to read .npy files
 */
object NumpyReaderScalaPy {
  
  /**
   * Read numpy file and return flattened float array
   */
  def readNumpyData(filename: String): Try[Array[Float]] = {
    Try {
      val np = py.module("numpy")
      val data = np.load(filename)
      val flatData = data.flatten()
      
      // Convert to Scala Array[Float] using tolist() first
      val listData = flatData.tolist()
      listData.as[Seq[Float]].toArray
    }
  }
  
  /**
   * Read numpy file and return metadata and data
   */
  def readNumpyFile(filename: String): Try[(NumpyMetadata, Array[Float])] = {
    Try {
      val np = py.module("numpy")
      val data = np.load(filename)
      
      // Extract metadata
      val shapeList = data.shape.tolist()
      val shape = shapeList.as[Seq[Int]].toArray
      
      val metadata = NumpyMetadata(
        dataType = data.dtype.name.as[String],
        shape = shape,
        fortranOrder = false, // Default to C order
        dataOffset = 0
      )
      
      // Get flattened data
      val flatData = data.flatten()
      val listData = flatData.tolist()
      val floatData = listData.as[Seq[Float]].toArray
      
      (metadata, floatData)
    }
  }
  
  /**
   * Get basic statistics about the data
   */
  def getDataStats(filename: String): Try[DataStats] = {
    Try {
      val np = py.module("numpy")
      val data = np.load(filename)
      val flatData = data.flatten()
      
      val totalElements = flatData.size.as[Int]
      val nonZeroCount = np.count_nonzero(flatData).as[Int]
      val zeroRatio = 1.0 - (nonZeroCount.toDouble / totalElements)
      
      DataStats(totalElements, nonZeroCount, zeroRatio)
    }
  }
  
  /**
   * Feed data in chunks for processing
   */
  def feedNumpyDataInChunks(filename: String, chunkSize: Int): Iterator[Array[Float]] = {
    val np = py.module("numpy")
    val data = np.load(filename)
    val flatData = data.flatten()
    val listData = flatData.tolist()
    val floatData = listData.as[Seq[Float]].toArray
    
    floatData.grouped(chunkSize)
  }
  
  /**
   * Feed data as a stream
   */
  def feedNumpyData(filename: String): Iterator[Float] = {
    val np = py.module("numpy")
    val data = np.load(filename)
    val flatData = data.flatten()
    val listData = flatData.tolist()
    val floatData = listData.as[Seq[Float]].toArray
    
    floatData.iterator
  }
  
  /**
   * Numpy file metadata
   */
  case class NumpyMetadata(
    dataType: String,
    shape: Array[Int],
    fortranOrder: Boolean,
    dataOffset: Int
  )
  
  /**
   * Data statistics
   */
  case class DataStats(
    totalElements: Int,
    nonZeroCount: Int,
    zeroRatio: Double
  )
} 