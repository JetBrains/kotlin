// KIND: STANDALONE_NO_TR
// ENTRY_POINT: foo.bar.altMain

// FILE: main.kt

package foo.bar.baz.qux

fun main() {
    error("Not supposed to be called")
}

// FILE: altMain.kt

package foo.bar

import kotlin.test.*

fun altMain() {
    assertEquals(42, 40 + 2)
    println("OK")
}
