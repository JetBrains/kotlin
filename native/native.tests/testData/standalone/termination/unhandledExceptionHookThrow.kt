// EXIT_CODE: !0
// OUTPUT_REGEX: Hook\nUncaught Kotlin exception: kotlin\.Error: another error\n(?!.*an error.*).*
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.test.*

fun main() {
    setUnhandledExceptionHook {
        println("Hook")
        throw Error("another error")
    }

    throw Error("an error")
}