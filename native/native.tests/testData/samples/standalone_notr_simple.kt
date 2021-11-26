// KIND: STANDALONE_NO_TR

import kotlin.test.*

fun main() {
    assertEquals(42, 40 + 2)
    println("OK")
}

fun altMain() {
    error("Not supposed to be called")
}
