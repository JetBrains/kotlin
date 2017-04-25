// EXPECTED_REACHABLE_NODES: 491
package foo

fun box(): String {

    assertEquals(65536L, 1L shl 16)
    assertEquals(1L, 65536L shr 16)

    assertEquals(-1L, -1L shr 48)
    assertEquals(65535L, -1L ushr 48)

    assertEquals(-1L, 0L.inv())

    assertEquals(0b1000L, 0b1100L and 0b1010L)
    assertEquals(0b1110L, 0b1100L or 0b1010L)
    assertEquals(0b0110L, 0b1100L xor 0b1010L)

    assertEquals(0xab88ac0021L, 0xabcdef0123L and 0xefaabcdef1L)
    assertEquals(0xefefffdff3L, 0xabcdef0123L or 0xefaabcdef1L)
    assertEquals(0x446753dfd2L, 0xabcdef0123L xor 0xefaabcdef1L)
    assertEquals(-737894400292, 0xabcdef0123L.inv())

    return "OK"
}