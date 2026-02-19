// TARGET_BACKEND: NATIVE
// WITH_PLATFORM_LIBS
@file:OptIn(ExperimentalForeignApi::class)

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.invoke
import kotlinx.cinterop.staticCFunction

fun same(value: Int) = value

fun box(): String {
    val function = staticCFunction { value: Int ->
        val callback = { same(value) }
        callback()
    }
    val result = function(42)
    return if (result == 42) "OK" else "FAIL: $result"
}
