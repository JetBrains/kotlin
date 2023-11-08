// EXIT_CODE: !0
// OUTPUT_REGEX: Uncaught Kotlin exception: kotlin\.Error: an error\n.*
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

fun main() {
    terminateWithUnhandledException(Error("an error"))
}