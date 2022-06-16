@file:OptIn(FreezingIsDeprecated::class)

import kotlin.native.concurrent.*

fun main() {
    Worker.current.executeAfter(0L, {
        throw Error("some error")
    }.freeze())
    Worker.current.processQueue()
    println("Will not happen")
}
