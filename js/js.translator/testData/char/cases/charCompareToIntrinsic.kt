package foo

fun box(): String {

    assertEquals(true, 64 < 'A')
    assertEquals(true, (64: Short) < 'A')
    assertEquals(true, (64: Byte) < 'A')
    assertEquals(true, 64L < 'A')
    assertEquals(true, 64.0 < 'A')
    assertEquals(true, 64.0f < 'A')

    assertEquals(true, 'A' > 64)
    assertEquals(true, 'A' > (64: Short))
    assertEquals(true, 'A' > (64: Byte))
    assertEquals(true, 'A' > 64L)
    assertEquals(true, 'A' > 64.0)
    assertEquals(true, 'A' > 64.0f)

    return "OK"
}