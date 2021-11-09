// KIND: STANDALONE_NO_TR

import kotlin.test.*

fun main(args: Array<String>) {
    assertEquals(42, 40 + 2)
    println("OK")
}

fun altMain(args: Array<String>) {
    error("Not supposed to be called")
}
