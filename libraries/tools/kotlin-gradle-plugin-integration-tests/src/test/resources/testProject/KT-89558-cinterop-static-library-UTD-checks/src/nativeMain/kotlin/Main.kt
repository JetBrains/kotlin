@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import lib.getGlobalCounter

fun main() {
    println(getGlobalCounter())
}
