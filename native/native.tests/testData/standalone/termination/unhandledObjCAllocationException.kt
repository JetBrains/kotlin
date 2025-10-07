// EXIT_CODE: !0
// OUTPUT_REGEX: Hook\R\*\*\* \+\[NSJSONSerialization allocWithZone:\](?!.*FAIL.*).*
// DISABLE_NATIVE: isAppleTarget=false
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.objc.*

fun customExceptionHook(exception: Any?): Unit {
    println("Hook")
    println(exception)
}

fun main() {
    objc_setUncaughtExceptionHandler(staticCFunction(::customExceptionHook))
    NSJSONSerialization()
    println("FAIL")
}