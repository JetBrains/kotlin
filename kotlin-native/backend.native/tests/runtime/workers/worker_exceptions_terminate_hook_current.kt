@file:OptIn(FreezingIsDeprecated::class, ExperimentalStdlibApi::class)

import kotlin.native.concurrent.*

fun main() {
    setUnhandledExceptionHook({ _: Throwable ->
        println("hook called")
    }.freeze())

    Worker.current.executeAfter(0L, {
        throw Error("some error")
    }.freeze())
    Worker.current.processQueue()
    println("Will happen")
}
