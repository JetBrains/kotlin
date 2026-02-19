// INPUT_DATA_FILE: readln.in

import kotlin.test.*

fun main() {
    assertEquals("first", readln())
    assertEquals("", readln())
    assertEquals("second", readln())
    // NOTE: Between `bbb` and `c` is "\r\n". `readln()` treats it as a single line separator on all platforms.
    assertContentEquals("aaaaaa\rbbb".encodeToByteArray(), readln().encodeToByteArray())
    assertEquals("c", readln())
    assertEquals("Привет!", readln())
    assertEquals("A very long line of input with length of more than initial buffer size", readln())
    assertNull(readlnOrNull())
    assertFailsWith<RuntimeException> { readln() }
}