// No termination is going on here. But that's the closest location to other unhandled exception hook tests.
// OUTPUT_DATA_FILE: unhandledExceptionHookWithProcess.out
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.concurrent.AtomicInt
import kotlin.test.*

fun main() {
    val exception = Error("an error")
    val called = AtomicInt(0)
    setUnhandledExceptionHook {
        assertSame(exception, it)
        called.value = 1
    }

    processUnhandledException(exception)
    assertEquals(1, called.value)
}