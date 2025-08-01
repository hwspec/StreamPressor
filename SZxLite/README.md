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

