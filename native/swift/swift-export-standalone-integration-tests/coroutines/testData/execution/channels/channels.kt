// IGNORE_NATIVE: targetFamily=IOS
// IGNORE_NATIVE: targetFamily=TVOS
// IGNORE_NATIVE: targetFamily=WATCHOS
// IGNORE_NATIVE: target=macos_x64
// KIND: STANDALONE
// MODULE: Main
// FILE: channels.kt

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

abstract class Elem

object Element1: Elem()
object Element2: Elem()
object Element3: Elem()

fun testRegular(): ReceiveChannel<Elem> {
    val channel = Channel<Elem>()
    CoroutineScope(Dispatchers.Default).launch {
        channel.send(Element1)
        channel.send(Element2)
        channel.send(Element3)
        channel.close()
    }
    return channel
}

fun testNullable(): ReceiveChannel<Elem?> {
    val channel = Channel<Elem?>()
    CoroutineScope(Dispatchers.Default).launch {
        channel.send(Element1)
        channel.send(null)
        channel.send(Element2)
        channel.send(null)
        channel.send(Element3)
        channel.close()
    }
    return channel
}

fun testEmpty(): ReceiveChannel<Elem> {
    val channel = Channel<Elem>()
    CoroutineScope(Dispatchers.Default).launch {
        channel.close()
    }
    return channel
}

fun testString(): ReceiveChannel<String> {
    val channel = Channel<String>()
    CoroutineScope(Dispatchers.Default).launch {
        channel.send("hello")
        channel.send("any")
        channel.send("world")
        channel.close()
    }
    return channel
}

fun testPrimitive(): ReceiveChannel<UInt> {
    val channel = Channel<UInt>()
    CoroutineScope(Dispatchers.Default).launch {
        channel.send(1u)
        channel.send(2u)
        channel.send(3u)
        channel.close()
    }
    return channel
}

fun testFailing(): ReceiveChannel<Elem> {
    val channel = Channel<Elem>()
    CoroutineScope(Dispatchers.Default).launch {
        channel.send(Element1)
        channel.send(Element2)
        channel.close(IllegalStateException("Channel has Failed"))
    }
    return channel
}
