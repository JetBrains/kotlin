// EXIT_CODE: !0
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// OUTPUT_REGEX: value 42: Error\. Runnable state: true\RUncaught Kotlin exception: kotlin\.Error: an error\R.*
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class)

import kotlin.native.runtime.Debugging
import kotlin.test.*

fun main() {
    val x = 42
    setUnhandledExceptionHook { throwable: Throwable ->
        println("value $x: ${throwable::class.simpleName}. Runnable state: ${Debugging.isThreadStateRunnable}")
    }

    throw Error("an error")
}