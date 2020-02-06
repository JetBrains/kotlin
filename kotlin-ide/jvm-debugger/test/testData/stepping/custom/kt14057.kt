package test

import java.io.StringReader

fun main() {
    val reader = StringReader("test")
    reader.readText()
}

// ADDITIONAL_BREAKPOINT: ReadWrite.kt / public fun Reader.copyTo( / fun
// STEP_OVER: 2
// STEP_INTO: 1