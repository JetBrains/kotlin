# Sockets demo

To build use `../gradlew build` or `./build.sh`.

Now you can run the server 

    ./build/exe/main/release/EchoServer.kexe 3000 &

Test the server by conecting to it, for example with telnet:

    telnet localhost 3000

Write something to console and watch server echoing it back.

~~Quit telnet by pressing ctrl+] ctrl+D~~

