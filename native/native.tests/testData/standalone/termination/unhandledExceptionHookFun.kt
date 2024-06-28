// EXIT_CODE: !0
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// OUTPUT_REGEX: value true: Error\. Runnable state: true\RUncaught Kotlin exception: kotlin\.Error: an error\R.*
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class)

import kotlin.native.runtime.Debugging
import kotlin.test.*

fun customExceptionHook(throwable: Throwable) {
    println("value ${getUnhandledExceptionHook() == ::customExceptionHook}: ${throwable::class.simpleName}. Runnable state: ${Debugging.isThreadStateRunnable}")
}

fun main() {
    val x = 42
    setUnhandledExceptionHook(::customExceptionHook)

    throw Error("an error")
}