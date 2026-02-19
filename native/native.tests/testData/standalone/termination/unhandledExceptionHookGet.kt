// No termination is going on here. But that's the closest location to other unhandled exception hook tests.
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.test.*

fun main() {
    val exceptionHook = { _: Throwable -> Unit }

    val oldHook = setUnhandledExceptionHook(exceptionHook)
    assertNull(oldHook)
    val hook1 = getUnhandledExceptionHook()
    assertEquals(exceptionHook, hook1)
    val hook2 = getUnhandledExceptionHook()
    assertEquals(exceptionHook, hook2)
    val hook3 = setUnhandledExceptionHook(null)
    assertEquals(exceptionHook, hook3)
    val hook4 = getUnhandledExceptionHook()
    assertNull(hook4)
}