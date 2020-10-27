# GIT frequency analyzer

This example shows how one could perform statistics on Git repository.

Install libgit2 development files.
For Debian-like Linux - use `apt-get install libgit2-dev`.
For Windows - `pacman -S mingw-w64-x86_64-libgit2` in MinGW64 console, if you do
not have MSYS2-MinGW64 installed - install it first as described in http://www.msys2.org

To build use `../gradlew assemble`.

To run use `../gradlew runReleaseExecutableGitChurn` or execute the program directly:

    ./build/bin/gitChurn/main/release/executable/gitchurn.kexe ../../

It will print most frequently modified (by number of commits) files in repository.
