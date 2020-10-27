# Sockets demo

To build use `../gradlew assemble`.

To run use `../gradlew runReleaseExecutableEchoServer` or execute the program directly:

    ./build/bin/echoServer/main/release/executable/echoServer.kexe 3000 &

Test the server by connecting to it, for example with telnet:

    telnet localhost 3000

Write something to console and watch server echoing it back.

~~Quit telnet by pressing ctrl+] ctrl+D~~
