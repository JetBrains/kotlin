@file:OptIn(FreezingIsDeprecated::class, kotlin.experimental.ExperimentalNativeApi::class, ObsoleteWorkersApi::class)

import kotlin.native.concurrent.*

fun main() {
    setUnhandledExceptionHook({ _: Throwable ->
        println("hook called")
    }.freeze())


    val worker = Worker.start()
    worker.executeAfter(0L, {
        throw Error("some error")
    }.freeze())
    worker.requestTermination().result
    println("Will happen")
}
