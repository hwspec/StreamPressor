# StreamPressor (last updated: 07/02/2025)

[![Build Status](https://github.com/kazutomo/StreamPressor/workflows/CI/badge.svg)](https://github.com/kazutomo/StreamPressor/actions)
[![License](https://img.shields.io/badge/License-Argonne%20National%20Lab-blue.svg)](LICENSE.txt)
[![Scala](https://img.shields.io/badge/Scala-2.13.10-red.svg)](https://www.scala-lang.org/)
[![Chisel](https://img.shields.io/badge/Chisel-3.5.6-orange.svg)](https://www.chisel-lang.org/)

StreamPressor is a **stream compressor hardware generator** written in the Chisel hardware construction language for evaluating various designs of streaming hardware compressors. The framework combines predefined hardware compressor primitives with user-defined primitives to generate Verilog code for simulation and integration with other hardware designs.

## 🚀 Features

- **Hardware Compression Pipeline**: Complete X-ray data compression pipeline from .npy files to compressed output
- **Bit Plane Compression**: Advanced bit plane analysis and compression algorithms
- **Lagrange Prediction**: Hardware implementation of Lagrange-based prediction for data compression
- **Variable-to-Fixed Conversion**: V2F and F2V converters for efficient data packing
- **Formal Verification**: Comprehensive formal testing with bounded model checking
- **Numpy Integration**: Direct support for reading and processing .npy files via ScalaPy
- **Bit Shuffling**: Optimized bit shuffling algorithms for improved compression ratios
- **Multi-format Support**: Support for both 32-bit and 64-bit floating-point data

## 📋 Prerequisites

- **Java SDK** (8 or 11 recommended)
- **sbt** (see [https://www.scala-sbt.org/download.html](https://www.scala-sbt.org/download.html) for installation)
- **Verilator** and **z3** (for formal verification)
- **Python 3** with **numpy** (for .npy file support)
- **Linux environment** is recommended 

### Installing Dependencies

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install -y z3 python3 python3-dev python3-pip verilator

# Install Python dependencies
pip3 install numpy

# Fedora/RHEL
sudo dnf install z3 python3 python3-devel python3-pip verilator
pip3 install numpy
```

## 🏃‍♂️ Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/kazutomo/StreamPressor.git
cd StreamPressor
```

### 2. Run Tests

```bash
# Run all tests
sbt test

# Run with formal verification enabled
sbt "testOnly -- -DFORMAL=1"

# Run specific test suite
sbt "testOnly common.XRayCompressionPipelineSpec"
```

### 3. Generate Verilog

```bash
# Generate Verilog for LPComp module
sbt 'runMain lpe.LPCompGen'

# List all available targets
sbt run
```

## 🛠️ Build and Development

### Basic Commands

```bash
# Compile and run tests
sbt test

# Generate Verilog codes for target modules
sbt run

# Run the compression ratio estimator
sbt 'runMain estimate.EstimateCR'

# Clean build artifacts
sbt clean
```

### Using the Makefile

The project includes a `Makefile` with convenient shortcuts:

```bash
# Run all tests
make test

# Run formal verification tests
make formal

# Run compression ratio estimator
make estimator

# Clean generated files
make clean
```

### Formal Testing

To enable formal verification testing:

```bash
# Run all formal tests
sbt "testOnly -- -DFORMAL=1"

# Run specific formal test
sbt "testOnly common.LagrangePredFormalSpec -- -DFORMAL=1"
```

## 📊 Project Structure

```
StreamPressor/
├── src/
│   ├── main/scala/
│   │   ├── common/                    # Core compression utilities
│   │   │   ├── BitPlaneCompressor.scala    # Bit plane analysis and compression
│   │   │   ├── BitShuffle.scala            # Bit shuffling algorithms
│   │   │   ├── BitShuffleUtils.scala       # Bit shuffle utilities
│   │   │   ├── ClzParam.scala              # Count leading zeros parameterized
│   │   │   ├── ConversionUtils.scala       # V2F/F2V conversion utilities
│   │   │   ├── DataFeeder.scala            # Data feeding and streaming
│   │   │   ├── F2VConv.scala               # Fixed-to-Variable converter
│   │   │   ├── F2VConv.scala               # Float-to-Vector converter
│   │   │   ├── Headers.scala               # Header definitions
│   │   │   ├── IntegerizeFP.scala          # Floating-point to integer conversion
│   │   │   ├── NumpyReaderScalaPy.scala    # Numpy file reading via ScalaPy
│   │   │   ├── Utils.scala                 # Utility functions
│   │   │   ├── V2FConv.scala               # Variable-to-Fixed converter
│   │   │   └── VFConv.scala                # Vector-to-Float converter
│   │   ├── configs/                    # Configuration modules
│   │   │   └── LPEComp.scala               # Lagrange prediction compression config
│   │   ├── estimate/                   # Compression ratio estimation
│   │   │   └── LPECompEstimateCR.scala     # Compression ratio estimator
│   │   └── lpe/                        # Lagrange prediction encoder/decoder
│   │       ├── LagrangePred.scala          # Lagrange prediction core
│   │       └── LPEncoder.scala             # Lagrange prediction encoder
│   └── test/scala/
│       ├── common/                      # Core component tests
│       │   ├── BitShuffleSpec.scala         # Bit shuffle tests
│       │   ├── ClzParamSpec.scala           # Count leading zeros tests
│       │   ├── ConvTestPats.scala           # Conversion test patterns
│       │   ├── DataFeederSpec.scala         # Data feeder tests
│       │   ├── F2VConvSpec.scala            # F2V converter tests
│       │   ├── IntegerizeFPSpec.scala       # IntegerizeFP tests
│       │   ├── Misc.scala                   # Miscellaneous tests
│       │   ├── NumpyReaderScalaPySpec.scala # Numpy reader tests
│       │   ├── V2FConvSpec.scala            # V2F converter tests
│       │   ├── V2FtoF2VSpec.scala           # V2F/F2V loopback tests
│       │   ├── V2FtoF2VTest.scala           # V2F/F2V integration tests
│       │   └── XRayCompressionPipelineSpec.scala # End-to-end pipeline tests
│       └── lpe/                         # Lagrange prediction tests
│           ├── LagrangePredSpec.scala       # Lagrange prediction tests
│           └── LPEncoderSpec.scala          # LP encoder tests
├── test_data/                        # Test data files
│   └── 25-trimmed.npy                   # X-ray test data (128KB)
├── misc/                             # Miscellaneous files
│   └── swimplforcomparison/           # SWIMPL comparison tools
│       ├── disasmtest.c                   # Disassembly test
│       ├── Makefile                       # Build configuration
│       ├── measuretiming.c                # Timing measurement
│       └── rdtsc.h                        # RDTSC header
├── .github/                          # GitHub configuration
│   └── workflows/
│       └── test.yml                      # CI/CD workflow
├── .gitignore                        # Git ignore rules (342 lines)
├── .scalafmt.conf                    # Scala code formatting rules
├── build.sbt                         # SBT build configuration
├── LICENSE.txt                       # Argonne National Lab license
├── Makefile                          # Build shortcuts and targets
└── README.md                         # This documentation file
```

## 🔬 Key Components

### Bit Plane Compression
- Analyzes data sparsity across bit planes
- Eliminates zero bit planes for compression
- Provides detailed compression statistics

### Lagrange Prediction
- Hardware implementation of Lagrange-based prediction
- Supports configurable coefficients
- Includes both encoder and decoder modules

### Variable-to-Fixed Conversion
- **V2FConv**: Converts variable-length data to fixed-size blocks
- **F2VConv**: Converts fixed-size blocks back to variable-length data
- Optimized for streaming data processing

### Numpy Integration
- Direct .npy file reading via ScalaPy
- Support for chunked data processing
- Data statistics and analysis tools

## 📈 Performance

The framework provides comprehensive compression analysis:

- **Compression Ratio**: Measures space savings achieved
- **Bit Plane Sparsity**: Analyzes data distribution across bit planes
- **Zero Suppression**: Tracks elimination of zero bit planes
- **Processing Throughput**: Hardware performance metrics

## 🧪 Testing

The project includes 38 comprehensive tests covering:

- **Unit Tests**: Individual component functionality
- **Integration Tests**: Complete pipeline testing
- **Formal Verification**: Bounded model checking
- **Performance Tests**: Compression ratio validation

### Test Categories

- `common.*Spec` - Core utility tests
- `lpe.*Spec` - Lagrange prediction tests
- `*FormalSpec` - Formal verification tests
- `XRayCompressionPipelineSpec` - End-to-end pipeline tests


### Development Guidelines

- Follow Scala and Chisel coding conventions
- Add tests for new functionality
- Update documentation for API changes
- Ensure all tests pass before submitting

## 📄 License

This project is licensed under the Argonne National Laboratory Open Source License - see the [LICENSE.txt](LICENSE.txt) file for details.

## 👥 Authors

- **Kazutomo Yoshii** - *Initial work* - [kazutomo@mcs.anl.gov](mailto:kazutomo@mcs.anl.gov)
- **Connor Bohannon** - *Documentation, testing, and X-ray compression features*

## 🙏 Acknowledgments

- Based on research from T. Ueno et al., "Bandwidth Compression of Floating-Point Numerical Data Streams for FPGA-based High-Performance Computing"
- Developed at Argonne National Laboratory
- Built with [Chisel](https://www.chisel-lang.org/) hardware construction language

## 📞 Support

For questions and support:
- Open an issue on GitHub
- Contact: [kazutomo@mcs.anl.gov](mailto:kazutomo@mcs.anl.gov)

---
