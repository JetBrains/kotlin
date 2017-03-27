# GIT frequency analyzer

 This example shows how one could perform statistics on Git repository.

To build use `./build.sh` script without arguments (or specify `TARGET` variable if cross-compiling).

To run use

    ./GitChurn.kexe <path-to-some-git-repo>

It will print most frequently modified (by number of commits) files in repository.
