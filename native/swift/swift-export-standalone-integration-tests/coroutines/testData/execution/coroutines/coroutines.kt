// IGNORE_NATIVE: targetFamily=IOS
// IGNORE_NATIVE: targetFamily=TVOS
// IGNORE_NATIVE: targetFamily=WATCHOS
// IGNORE_NATIVE: target=macos_x64
// KIND: STANDALONE
// MODULE: Main
// FILE: coroutines.kt

import kotlinx.coroutines.*

object Foo

fun testPrimitiveProducedLambda(): suspend ()->Int = ::testPrimitive

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

suspend fun cancelImmediately(): Int {
    val reason = CancellationException("Cancelled")
    currentCoroutineContext().cancel(reason)
    throw reason
}

suspend fun throwAfter(delay: Long, message: String): Int {
    delay(delay)
    error(message)
}

suspend fun throwImmediately(message: String): Int {
    error(message)
}

suspend fun throwNonException(message: String): Int {
    class NonExceptionThrowable(message: String) : Throwable(message)
    throw NonExceptionThrowable(message)
}

suspend fun neverCompletes(): Int = coroutineScope {
    suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation {}
    }
}

suspend fun finallyDelayInt(delay: Long, onFinally: (() -> Unit)?): Int {
    var result = 0
    try {
        delay(delay)
        return 67
    } finally {
        onFinally?.invoke()
    }
    return result
}

suspend fun completeTwiceSuccessThenSuccessDeterministic(): Int {
    val gate = CompletableDeferred<Int>()
    gate.complete(42)
    gate.complete(43) // must be ignored
    return gate.await()
}

suspend fun completeTwiceSuccessThenThrowDeterministic(message: String): Int {
    val gate = CompletableDeferred<Int>()
    gate.complete(42)
    gate.completeExceptionally(IllegalStateException(message)) // must be ignored
    return gate.await()
}

suspend fun completeTwiceThrowThenSuccessDeterministic(message: String): Int {
    val gate = CompletableDeferred<Int>()
    gate.completeExceptionally(IllegalStateException(message))
    gate.complete(42) // must be ignored
    return gate.await() // throws
}

suspend fun completeTwiceCancelThenSuccessDeterministic(): Int {
    val gate = CompletableDeferred<Int>()
    gate.completeExceptionally(CancellationException("cancel-first"))
    gate.complete(42) // must be ignored
    return gate.await() // throws CancellationException -> Swift CancellationError
}
