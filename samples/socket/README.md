# Sockets demo

Compile the echo server (in EAP only supported on Mac host):

    ./build.sh

Run the server:

    ./EchoServer.kexe 3000 &

Test the server by conecting to it, for example with telnet:

    telnet localhost 3000

Write something to console and watch server echoing it back.

~~Quit telnet by pressing ctrl+] ctrl+D~~

