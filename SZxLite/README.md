### PLEASE READ 
*Notes how to set up the chipyard enviroment with the SZxLite Codes 
Assuming you have chipyard enviroment setup 
https://chipyard.readthedocs.io/en/latest/Chipyard-Basics/Initial-Repo-Setup.html

When adding the scala code you will need to add some directorys to chipyard as the setup is not very linear 

Steps (in no particular order right now) 

cd generators/
mkdir rocc/src/main/scala/szx

move SZxBlockProcessor.scala & SZxRoCCAccelerator.scala to the new directory you just made 

go back to chipyard/generators/rocc

add the build.sbt here in this directory 

next 

cd generators/chipyard/src/main/scala/config 

here you will move SZxRoCCConfig.scala to this directory 

back in the main directory 

cd ~/chipyard 

you want to vim/nano into the built.sbt 

vim built.sbt 

Looking around line 255-265 you want to look for lines lazy vall rerocc and rocc_acc_utils 

in between these two snippets of code you want to add this snippet 
```
lazy val rocc = (project in file("generators/rocc"))
  .dependsOn(rocketchip)
  .settings(libraryDependencies ++= rocketLibDeps.value)
  .settings(commonSettings)
```
then further up in the same build.sbt you want to add you new name to the .dependsOn 
the snippet will look like this (added in bold) 
```
...
lazy val chipyard = (project in file("generators/chipyard"))
  .dependsOn(testchipip, rocketchip, boom, rocketchip_blocks, rocketchip_inclusive_cache,
    dsptools, rocket_dsp_utils,
    gemmini, icenet, tracegen, cva6, nvdla, sodor, ibex, fft_generator,
    constellation, mempress, barf, shuttle, caliptra_aes, rerocc, **rocc**,
    compressacc, saturn, ara, firrtl2_bridge, vexiiriscv)
...
```


