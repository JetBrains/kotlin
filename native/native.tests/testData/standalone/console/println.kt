// OUTPUT_DATA_FILE: println.out
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.toKString

fun main() {
    println("Hello, world!")
    println(123.toByte())
    println(239)
    println(true)
    println(3.14159)
    println('A')
    println("Привет")
    println("\uD800\uDC00")
    println("")

    // Illegal surrogate pair -> default output
    println("\uDC00\uD800")
    // Lone surrogate -> default output
    println("\uD80012")
    println("\uDC0012")
    println("12\uD800")

    // https://github.com/JetBrains/kotlin-native/issues/1091
    val array = byteArrayOf(0xF0.toByte(), 0x9F.toByte(), 0x98.toByte(), 0xA5.toByte())
    array.decodeToString().apply { println(this) }
    array.toKString().apply { println(this) }
}