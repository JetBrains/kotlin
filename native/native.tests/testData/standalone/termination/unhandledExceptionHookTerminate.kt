// EXIT_CODE: !0
// OUTPUT_REGEX: Hook\nUncaught Kotlin exception: kotlin\.Error: an error\n.*
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.test.*

fun main() {
    setUnhandledExceptionHook {
        println("Hook")
        terminateWithUnhandledException(it)
    }

    throw Error("an error")
}