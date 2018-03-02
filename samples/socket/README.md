# Sockets demo

To build use `../gradlew build` or `./build.sh`.

Run the server:

    ../gradlew run
    
To change run arguments, change property runArgs in gradle.propeties file 
or pass `-PrunArgs="3000"` to gradle run. 

Alternatively you can run artifact directly 

    ./build/konan/bin/<platform>/EchoServer.kexe 3000 &

Test the server by conecting to it, for example with telnet:

    telnet localhost 3000

Write something to console and watch server echoing it back.

~~Quit telnet by pressing ctrl+] ctrl+D~~

