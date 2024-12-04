// KIND: STANDALONE_NO_TR

import kotlin.test.assertEquals

// Prints 1_000_000 bytes to stdout and exits.
fun main() {
    assertEquals(10, TEN_BYTES_STRING.length)
    repeat(1000) { print1000Bytes() }
}

private fun print1000Bytes() {
    // Print 1000 bytes.
    repeat(100) { print(TEN_BYTES_STRING) }
}

private const val TEN_BYTES_STRING = "Hi, test!\n"
