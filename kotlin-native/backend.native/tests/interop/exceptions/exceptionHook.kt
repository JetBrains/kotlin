@file:OptIn(FreezingIsDeprecated::class, kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.native.concurrent.freeze

// KT-47828
fun setHookAndThrow() {
    val hook = { _: Throwable ->
        println("OK. Kotlin unhandled exception hook")
    }
    hook.freeze()
    setUnhandledExceptionHook(hook)
    throw Exception("Error")
}