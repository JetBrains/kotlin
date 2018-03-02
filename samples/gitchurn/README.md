# GIT frequency analyzer

This example shows how one could perform statistics on Git repository.
libgit2 is required for this to work (`apt-get install libgit2-dev`).

To build use `../gradlew build` or `./build.sh`.

To run use `../gradlew run`.

To change run arguments, change property runArgs in gradle.propeties file 
or pass `-PrunArgs="../../"` to gradle run. 

Alternatively you can run artifact directly 

    ./build/konan/bin/<target>/GitChurn.kexe ../../

It will print most frequently modified (by number of commits) files in repository.
