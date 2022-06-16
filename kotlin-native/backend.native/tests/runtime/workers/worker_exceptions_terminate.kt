@file:OptIn(FreezingIsDeprecated::class)

import kotlin.native.concurrent.*

fun main() {
    val worker = Worker.start()
    worker.executeAfter(0L, {
        throw Error("some error")
    }.freeze())
    worker.requestTermination().result
    println("Will not happen")
}
