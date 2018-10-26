# GIT frequency analyzer

This example shows how one could perform statistics on Git repository.
libgit2 is required for this to work (`apt-get install libgit2-dev`).

To build use `../gradlew assemble`.

To run use `../gradlew runProgram` or execute the program directly:

    ./build/bin/gitChurn/main/release/executable/gitchurn.kexe ../../

It will print most frequently modified (by number of commits) files in repository.
