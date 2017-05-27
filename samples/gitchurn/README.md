# GIT frequency analyzer

 This example shows how one could perform statistics on Git repository.
libgit2 is required for this to work (`apt-get install libgit2-dev`).

To build use `./build.sh` script without arguments (or specify `TARGET` variable if cross-compiling).
You also may use Gradle to build this sample: `../gradlew build`.

To run use

    ./GitChurn.kexe <path-to-some-git-repo>

It will print most frequently modified (by number of commits) files in repository.
