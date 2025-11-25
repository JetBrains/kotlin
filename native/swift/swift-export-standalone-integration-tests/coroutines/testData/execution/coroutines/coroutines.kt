// IGNORE_NATIVE: targetFamily=IOS
// IGNORE_NATIVE: targetFamily=TVOS
// IGNORE_NATIVE: targetFamily=WATCHOS
// IGNORE_NATIVE: target=macos_x64
// KIND: STANDALONE
// MODULE: Main
// FILE: coroutines.kt

import kotlinx.coroutines.*

object Foo

suspend fun testPrimitive(): Int {
    delay(33L)
    return 42
}

suspend fun testAny(): Any {
    delay(33L)
    return Foo
}

suspend fun testObject(): Foo {
    delay(33L)
    return Foo
}

suspend fun testCustom(): String {
    delay(33L)
    return "Hello, World!"
}

suspend fun callAfter(delay: Long, callback: () -> Int): Int {
    delay(delay)
    return callback()
}

suspend fun cancelAfter(delay: Long): Int {
    delay(delay)
    val reason = CancellationException("Cancelled after $delay")
    currentCoroutineContext().cancel(reason)
    throw reason
}

suspend fun cancelSilentlyAfter(delay: Long, callback: () -> Int): Int {
    delay(delay)
    currentCoroutineContext().cancel()
    return callback()
}