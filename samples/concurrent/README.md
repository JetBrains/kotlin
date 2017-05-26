# Concurrent

This example shows how to implement concurrent programming in Kotlin/Native.
In this example we start multiple threads running concurrently and exchange messages with them.

To build cpp use `./buildCpp.sh`.
To build kotlin use `../gradlew build`.

To run use `../gradlew run`

Alternatively you can run artifact directly 

    ./build/konan/bin/MessageChannel/MessageChannel.kexe

It will print all passed messages.
