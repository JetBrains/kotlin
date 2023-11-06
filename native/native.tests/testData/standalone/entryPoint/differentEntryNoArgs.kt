// OUTPUT_DATA_FILE: differentEntry.out
// ENTRY_POINT: foo

import kotlin.test.*

fun foo() {
    println("OK")
}

fun foo(args: Array<Int>) {
    fail()
}

fun bar() {
    fail()
}

fun main(args: Array<String>) {
    fail()
}
