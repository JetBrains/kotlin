// EXPECTED_REACHABLE_NODES: 499
package foo

var global: String = ""

fun id(s: String, value: Int): Int {
    global += s
    return value
}

fun box(): String {

    assertEquals(-1, 1.compareTo(2))
    assertEquals(-1, (1 as Comparable<Int>).compareTo(2))

    assertEquals(-1, 1.compareTo(2L))
    assertEquals(-1, 1.compareTo(2L))
    assertEquals(-1, 1.compareTo(7540113804746346429L))
    assertEquals(-1, 1.compareTo(2.toShort()))
    assertEquals(-1, 1.compareTo(2.toByte()))
    assertEquals(-1, 1.compareTo(2.0))
    assertEquals(-1, 1.compareTo(2.0f))

    assertEquals(1, 10.compareTo(2L))
    assertEquals(1, 10.compareTo(2))
    assertEquals(1, 10.compareTo(2.toShort()))
    assertEquals(1, 10.compareTo(2.toByte()))
    assertEquals(1, 10.compareTo(2.0))
    assertEquals(1, 10.compareTo(2.0f))

    assertEquals(0, 2.compareTo(2L))
    assertEquals(0, 2.compareTo(2))
    assertEquals(0, 2.compareTo(2.toShort()))
    assertEquals(0, 2.compareTo(2.toByte()))
    assertEquals(0, 2.compareTo(2.0))
    assertEquals(0, 2.compareTo(2.0f))

    assertEquals(-1, 1.toShort().compareTo(2L))
    assertEquals(-1, 1.toShort().compareTo(7540113804746346429L))
    assertEquals(-1, 1.toShort().compareTo(2))
    assertEquals(-1, 1.toShort().compareTo(2.toShort()))
    assertEquals(-1, (1.toShort() as Comparable<Short>).compareTo(2.toShort()))
    assertEquals(-1, 1.toShort().compareTo(2.toByte()))
    assertEquals(-1, 1.toShort().compareTo(2.0))
    assertEquals(-1, 1.toShort().compareTo(2.0f))

    assertEquals(1, 10.toByte().compareTo(2L))
    assertEquals(-1, 10.toByte().compareTo(7540113804746346429L))
    assertEquals(1, 10.toByte().compareTo(2))
    assertEquals(1, 10.toByte().compareTo(2.toShort()))
    assertEquals(1, 10.toByte().compareTo(2.toByte()))
    assertEquals(1, (10.toByte() as Comparable<Byte>).compareTo(2.toByte()))
    assertEquals(1, 10.toByte().compareTo(2.0))
    assertEquals(1, 10.toByte().compareTo(2.0f))

    assertEquals(0, 2.0.compareTo(2L))
    assertEquals(-1, 2.0.compareTo(7540113804746346429L))
    assertEquals(0, 2.0.compareTo(2))
    assertEquals(0, 2.0.compareTo(2.toShort()))
    assertEquals(0, 2.0.compareTo(2.toByte()))
    assertEquals(0, 2.0.compareTo(2.0))
    assertEquals(0, (2.0 as Comparable<Double>).compareTo(2.0))
    assertEquals(0, 2.0.compareTo(2.0f))

    assertEquals(1, 3.0f.compareTo(2L))
    assertEquals(-1, 3.0f.compareTo(7540113804746346429L))
    assertEquals(1, 3.0f.compareTo(2))
    assertEquals(1, 3.0f.compareTo(2.toShort()))
    assertEquals(1, 3.0f.compareTo(2.toByte()))
    assertEquals(1, 3.0f.compareTo(2.0))
    assertEquals(1, 3.0f.compareTo(2.0f))
    assertEquals(1, (3.0f as Comparable<Float>).compareTo(2.0f))

    assertEquals(1, 10L.compareTo(2L))
    assertEquals(-1, 10L.compareTo(7540113804746346429L))
    assertEquals(1, (10L as Comparable<Long>).compareTo(2L))
    assertEquals(1, 10L.compareTo(2))
    assertEquals(1, 10L.compareTo(2.toShort()))
    assertEquals(1, 10L.compareTo(2.toByte()))
    assertEquals(1, 10L.compareTo(2.0))
    assertEquals(1, 10L.compareTo(2.0f))

    assertEquals(0, 'A'.compareTo('A'))

    assertEquals(-1, 1L.compareTo(2L))
    assertEquals(-1, 1L.compareTo(2))
    assertEquals(-1, 1L.compareTo(2.toShort()))
    assertEquals(-1, 1L.compareTo(2.toByte()))
    assertEquals(-1, 1L.compareTo(2.0))
    assertEquals(-1, 1L.compareTo(2.0f))

    assertEquals(0, 7540113804746346429L.compareTo(7540113804746346429L))
    assertEquals(1, 7540113804746346429L.compareTo(2L))
    assertEquals(0, 2L.compareTo(2L))
    assertEquals(0, 2L.compareTo(2))
    assertEquals(0, 2L.compareTo(2.toShort()))
    assertEquals(0, 2L.compareTo(2.toByte()))
    assertEquals(0, 2L.compareTo(2.0))
    assertEquals(0, 2L.compareTo(2.0f))

    assertEquals(1, 10L.compareTo(2L))
    assertEquals(1, 10L.compareTo(2))
    assertEquals(1, 10L.compareTo(2.toShort()))
    assertEquals(1, 10L.compareTo(2.toByte()))
    assertEquals(1, 10L.compareTo(2.0))
    assertEquals(1, 10L.compareTo(2.0f))

    assertEquals(-1, 1L.compareTo(2L))
    assertEquals(-1, 1L.compareTo(2))
    assertEquals(-1, 1L.compareTo(2.toShort()))
    assertEquals(-1, 1L.compareTo(2.toByte()))
    assertEquals(-1, 1L.compareTo(2.0))
    assertEquals(-1, 1L.compareTo(2.0f))

    assertEquals(0, 2L.compareTo(2L))
    assertEquals(0, 2L.compareTo(2))
    assertEquals(0, 2L.compareTo(2.toShort()))
    assertEquals(0, 2L.compareTo(2.toByte()))
    assertEquals(0, 2L.compareTo(2.0))
    assertEquals(0, 2L.compareTo(2.0f))

    assertEquals(1, id("A", 10).compareTo(id("B", 5)))
    assertEquals("AB", global)

    return "OK"
}