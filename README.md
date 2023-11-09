StreamPressor

StreamPressor is stream compressor hardware generator written in the
Chisel hardware construction langauge for evaluating various designs
of streaming hardware compressor that has combinations of predefined
hardware compressor primitives as well as user-defined primitives. The
framework generates Verilog codes from Chisel descriptions for
simulation and integration with other hardware designs.

# Prerequisites
- Java SDK
- sbt (see https://www.scala-sbt.org/download.html for installation)
- Verilator and z3
- (optional) Scala
- Linux environment is recommended (Tested on Fedora 35)


# Getting Started
---------------

     $ git clone https://github.com/kazutomo/sccomponents
     $ cd sccomponents
     $ sbt test

# Build and run:

Type 'sbt test' compiles the compressor and run tests.

Type 'sbt run' to generate Verilog codes for target module or run the estimator tool (sbt will show a list of targets).

$ 'runMain lpe.LPCompGen'   // to generate Verilog codes for LPComp

$ sbt 'runMain estimate.EstimateCR'   // to run the estimator tool

To enable formal testing:
$ sbt "testOnly -- -DFORMAL=1"

To run specific format test:
$ sbt "testOnly foobar.LagrangePredFormalSpec -- -DFORMAL=1"

We also provide Makefile that defines short-cut. Please take a look at Makefile.
