// KIND: STANDALONE_NO_TR
// ENTRY_POINT: foo.bar.altMain

package foo.bar

import kotlin.test.*

fun main() {
    error("Not supposed to be called")
}

fun altMain() {
    assertEquals(42, 40 + 2)
    println("OK")
}
