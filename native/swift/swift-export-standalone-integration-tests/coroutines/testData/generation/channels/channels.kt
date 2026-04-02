// IGNORE_NATIVE: targetFamily=IOS
// IGNORE_NATIVE: targetFamily=TVOS
// IGNORE_NATIVE: targetFamily=WATCHOS
// IGNORE_NATIVE: target=macos_x64
// KIND: STANDALONE
// APPLE_ONLY_VALIDATION
// MODULE: main
// FILE: channels.kt
import kotlinx.coroutines.channels.*

class Foo

fun produceChannel(): ReceiveChannel<Foo> = TODO()

fun consumeChannel(channel: ReceiveChannel<Foo>): Unit = TODO()

fun produceNullableChannel(): ReceiveChannel<Foo?> = TODO()

fun produceIntChannel(): ReceiveChannel<Int> = TODO()

fun produceStringChannel(): ReceiveChannel<String> = TODO()
