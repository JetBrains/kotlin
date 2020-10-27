# Non-blocking echo server demo

This sample shows how to implement multi-client server using coroutines.
IO operations are implemented using non-blocking OS calls, and instead coroutines
are being suspended and resumed whenever relevant.

Thus, while server can process multiple connections concurrently,
each individual connection handler is written in simple linear manner.

To build use `../gradlew assemble`.

To run use `../gradlew runReleaseExecutableNonBlockingEchoServer` or execute the program directly:

    ./build/bin/nonBlockingEchoServer/main/release/executable/nonBlockingEchoServer.kexe 3000 &

Test the server by connecting to it, for example with telnet:

    telnet localhost 3000

Write something to console and watch server echoing it back.
Concurrently connect from another terminal. Note that each connection gets its own
connection id prefixed to echo response.

~~Quit telnet by pressing ctrl+] ctrl+D~~

