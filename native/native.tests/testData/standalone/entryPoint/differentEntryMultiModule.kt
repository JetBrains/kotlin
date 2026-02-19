// OUTPUT_DATA_FILE: differentEntryMultiModule.out
// ENTRY_POINT: foo

// MODULE: lib
// FILE: lib.kt

fun foo(args: Array<String>) {
    println("OK")
}

// MODULE: program(lib)
// FILE: main.kt

import kotlin.test.*

fun main(args: Array<String>) {
    fail()
}