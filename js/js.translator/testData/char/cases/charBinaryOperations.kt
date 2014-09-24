package foo

fun box(): String {

    assertEquals(75, 10 + 'A')
    assertEquals(75, (10: Short) + 'A')
    assertEquals(75, (10: Byte) + 'A')
    assertEquals(75.0, 10.0 + 'A')
    assertEquals(75.0, 10.0f + 'A')
    assertEquals(75L, 10L + 'A')

    assertEquals(75, 'A' + 10)
    assertEquals(75, 'A' + (10: Short))
    assertEquals(75, 'A' + (10: Byte))
    assertEquals(75.0, 'A' + 10.0)
    assertEquals(75.0, 'A' + 10.0f)
    assertEquals(75L, 'A' + 10L)

    assertEquals(-55, 10 - 'A')
    assertEquals(-55, (10: Short) - 'A')
    assertEquals(-55, (10: Byte) - 'A')
    assertEquals(-55.0, 10.0 - 'A')
    assertEquals(-55.0, 10.0f - 'A')
    assertEquals(-55L, 10L - 'A')

    assertEquals(55, 'A' - 10)
    assertEquals(55, 'A' - (10: Short))
    assertEquals(55, 'A' - (10: Byte))
    assertEquals(55.0, 'A' - 10.0)
    assertEquals(55.0, 'A' - 10.0f)
    assertEquals(55L, 'A' - 10L)

    assertEquals(650, 10 * 'A')
    assertEquals(650, (10: Short) * 'A')
    assertEquals(650, (10: Byte) * 'A')
    assertEquals(650.0, 10.0 * 'A')
    assertEquals(650.0, 10.0f * 'A')
    assertEquals(650L, 10L * 'A')

    assertEquals(650, 'A' * 10)
    assertEquals(650, 'A' * (10: Short))
    assertEquals(650, 'A' * (10: Byte))
    assertEquals(650.0, 'A' * 10.0)
    assertEquals(650.0, 'A' * 10.0f)
    assertEquals(650L, 'A' * 10L)

    assertEquals(1, 100 / 'A')
    assertEquals(1, (100: Short) / 'A')
    assertEquals(1, (100: Byte) / 'A')
    assertEquals(100.0 / 65.0, 100.0 / 'A')
    assertEquals(100.0 / 65.0, 100.0f / 'A')
    assertEquals(1L, 100L / 'A')

    assertEquals(6, 'A' / 10)
    assertEquals(6, 'A' / (10: Short))
    assertEquals(6, 'A' / (10: Byte))
    assertEquals(6.5, 'A' / 10.0)
    assertEquals(6.5, 'A' / 10.0f)
    assertEquals(6L, 'A' / 10L)

    assertEquals(35, 100 % 'A')
    assertEquals(35, (100: Short) % 'A')
    assertEquals(35, (100: Byte) % 'A')
    // TODO Uncomment when KT-5860 (Double.mod(Char) is absent) will be fixed
    //assertEquals(35.0, 100.0 % 'A')
    assertEquals(35.0, 100.0f % 'A')
    assertEquals(35L, 100L % 'A')

    assertEquals(5, 'A' % 10)
    assertEquals(5, 'A' % (10: Short))
    assertEquals(5, 'A' % (10: Byte))
    assertEquals(5.0, 'A' % 10.0)
    assertEquals(5.0, 'A' % 10.0f)
    assertEquals(5L, 'A' % 10L)

    return "OK"
}