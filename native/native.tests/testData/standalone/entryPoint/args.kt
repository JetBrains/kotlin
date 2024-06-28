// PROGRAM_ARGS: AAA BB C

import kotlin.test.*

fun main(args: Array<String>) {
    assertContentEquals(arrayOf("AAA", "BB", "C"), args)
}