# GIT frequency analyzer

This example shows how one could perform statistics on Git repository.
libgit2 is required for this to work (`apt-get install libgit2-dev`).

To build use `../gradlew assemble` or `./build.sh`.

Now you can run the program directly 

    ./build/exe/main/release/<platform>/gitchurn.kexe ../../

It will print most frequently modified (by number of commits) files in repository.
