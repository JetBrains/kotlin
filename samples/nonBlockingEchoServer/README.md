# Non-blocking echo server demo

This sample shows how to implement multi-client server using coroutines.
IO operations are implemented using non-blocking OS calls, and instead coroutines
are being suspended and resumed whenever relevant.

Thus, while server can process multiple connections concurrently,
each individual connection handler is written in simple linear manner.

Compile the echo server (in EAP only supported on Mac host):

    ./build.sh

You also may use Gradle to build the server:

    ../gradlew build

Run the server:

    ./EchoServer.kexe 3000 &

Test the server by conecting to it, for example with telnet:

    telnet localhost 3000

Write something to console and watch server echoing it back.

~~Quit telnet by pressing ctrl+] ctrl+D~~

