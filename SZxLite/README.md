# Integrating `SZxLite` RoCC Accelerator into Chipyard

This guide explains how to integrate the custom `SZxLite` RoCC accelerator into a Chipyard setup.

> ⚠️ **Prerequisite**: Chipyard must already be cloned and initialized. If not, follow the official [Chipyard Initial Repo Setup](https://chipyard.readthedocs.io/en/latest/Chipyard-Basics/Initial-Repo-Setup.html).

---

## 🧱 Directory Structure Setup

1. Go to the Chipyard generators directory:
   ```bash
   cd chipyard/generators
   ```

2. Create the target directory for your accelerator code:
   ```bash
   mkdir -p rocc/src/main/scala/szx
   ```

3. Move your custom Scala files into this new path:
   ```bash
   mv path/to/SZxBlockProcessor.scala rocc/src/main/scala/szx/
   mv path/to/SZxRoCCAccelerator.scala rocc/src/main/scala/szx/
   ```

4. Create a `build.sbt` in `generators/rocc`:
   ```scala
   name := "rocc"

   version := "1.0"

   libraryDependencies ++= Seq(
     "edu.berkeley.cs" %% "rocketchip" % "1.2.+"
   )

   resolvers ++= Seq(
     Resolver.sonatypeRepo("snapshots"),
     Resolver.sonatypeRepo("releases"),
     Resolver.mavenLocal
   )
   ```

---

## ⚙️ Configuration File Placement

5. Move your custom config file into Chipyard:
   ```bash
   mv path/to/SZxRoCCConfig.scala generators/chipyard/src/main/scala/config/
   ```

---

## 🛠 Modifying Chipyard’s Main `build.sbt`

6. Open Chipyard’s root `build.sbt`:
   ```bash
   cd ~/chipyard
   vim build.sbt
   ```

7. Locate lines around 255–265. Between the definitions of `lazy val rerocc` and `rocc_acc_utils`, insert:
   ```scala
   lazy val rocc = (project in file("generators/rocc"))
     .dependsOn(rocketchip)
     .settings(libraryDependencies ++= rocketLibDeps.value)
     .settings(commonSettings)
   ```

8. Scroll up to the `lazy val chipyard = ...` section. Add `rocc` to the `.dependsOn(...)` list:
   ```scala
   lazy val chipyard = (project in file("generators/chipyard"))
     .dependsOn(testchipip, rocketchip, boom, rocketchip_blocks, rocketchip_inclusive_cache,
       dsptools, rocket_dsp_utils,
       gemmini, icenet, tracegen, cva6, nvdla, sodor, ibex, fft_generator,
       constellation, mempress, barf, shuttle, caliptra_aes, rerocc, rocc,
       compressacc, saturn, ara, firrtl2_bridge, vexiiriscv)
   ```

---

## 🧪 Add and Register Tests

9. Create a directory for your C-based tests:
   ```bash
   cd ~/chipyard/tests
   mkdir szx
   ```

10. Move all your SZx C tests into the new folder:
   ```bash
   mv path/to/c/tests/* szx/
   ```

11. Edit the `CMakeLists.txt` file in the `tests/` directory:
   ```bash
   vim CMakeLists.txt
   ```

12. Under the **Build** section, add:
   ```cmake
   #################################
   # SZx Test (Add Subdirectory)
   #################################
   add_subdirectory(szx)
   ```

13. At the bottom, under the **Disassembly** section, add:
   ```cmake
   # disassembly targets for SZx test
   add_dump_target(szx_compress_hw)
   ```

---

## ✅ Build & Run

Once setup is complete:

- Build your design:
- cd ~/chipyard/tests

```cmake
# file:  CMakeLists.txt
#
# usage:
#   Edit "VARIABLES"-section to suit project requirements.
#   Build instructions:
#     cmake -S ./ -B ./build/ -D CMAKE_BUILD_TYPE=Debug
#     cmake --build ./build/ --target szx_compress_hw
#   Cleaning:
#     cmake --build ./build/ --target clean
```

- cd ~/chipyard/sims/verilator

- Run your test:
  ```bash
  make CONFIG=SZxRoCCConfig run-binary BINARY=tests/szx/szx_compress_hw.riscv LOADMEM=1
  ```

---

---

## 📝 Notes & Tips

- ✅ Make sure your `SZxRoCCConfig.scala` correctly sets the `BuildRoCC` parameter to instantiate your `SZxRoCCAccelerator`.
- 📁 Simulation and test output files are typically located under `sim/generated-src` or `output/` after a successful run.
- 🧰 Want to explore powerful Chipyard internals? Open the `variables.mk` file in the root of Chipyard:
  ```bash
  vim ~/chipyard/variables.mk
  ```
  This file contains Makefile variables that are **shared across the entire Chipyard build system**. It's a great reference to:
  - Understand how targets are constructed
  - Discover useful environment variables
  - Customize build paths and toolchain behavior

---

## 📚 Additional Resources

- 🔗 **Official Chipyard Documentation**:  
  https://chipyard.readthedocs.io/en/latest/index.html

---

Happy hacking — and may your pipelines be fast, your clocks tight, and your accelerators screaming! 🧠⚙️🚀

