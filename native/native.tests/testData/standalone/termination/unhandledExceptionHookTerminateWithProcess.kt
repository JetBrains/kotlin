// EXIT_CODE: !0
// OUTPUT_REGEX: Hook\RUncaught Kotlin exception: kotlin\.Error: an error\R(?!.*FAIL.*).*
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.test.*

fun main() {
    setUnhandledExceptionHook {
        println("Hook")
        terminateWithUnhandledException(it)
    }

    processUnhandledException(Error("an error"))
    println("FAIL")
}