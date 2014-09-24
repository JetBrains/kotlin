package foo

var global: String = ""

fun id(s: String, value: Int): Int {
    global += s
    return value
}

fun box(): String {

    assertEquals(-1, 1.compareTo(2))
    assertEquals(-1, (1: Comparable<Int>).compareTo(2))

    assertEquals(-1, 1 compareTo 2L)
    assertEquals(-1, 1 compareTo 2L)
    assertEquals(-1, 1 compareTo 7540113804746346429L)
    assertEquals(-1, 1 compareTo (2:Short))
    assertEquals(-1, 1 compareTo (2:Byte))
    assertEquals(-1, 1 compareTo 2.0)
    assertEquals(-1, 1 compareTo 2.0f)
    assertEquals(-1, 1 compareTo 'A')

    assertEquals(1, 10 compareTo 2L)
    assertEquals(1, 10 compareTo 2)
    assertEquals(1, 10 compareTo (2:Short))
    assertEquals(1, 10 compareTo (2:Byte))
    assertEquals(1, 10 compareTo 2.0)
    assertEquals(1, 10 compareTo 2.0f)

    assertEquals(0, 2 compareTo 2L)
    assertEquals(0, 2 compareTo 2)
    assertEquals(0, 2 compareTo (2:Short))
    assertEquals(0, 2 compareTo (2:Byte))
    assertEquals(0, 2 compareTo 2.0)
    assertEquals(0, 2 compareTo 2.0f)

    assertEquals(-1, (1: Short) compareTo 2L)
    assertEquals(-1, (1: Short) compareTo 7540113804746346429L)
    assertEquals(-1, (1: Short) compareTo 2)
    assertEquals(-1, (1: Short) compareTo (2:Short))
    assertEquals(-1, ((1: Short): Comparable<Short>) compareTo (2:Short))
    assertEquals(-1, (1: Short) compareTo (2:Byte))
    assertEquals(-1, (1: Short) compareTo 2.0)
    assertEquals(-1, (1: Short) compareTo 2.0f)
    assertEquals(-1, (1: Short) compareTo 'A')

    assertEquals(1, (10: Byte) compareTo 2L)
    assertEquals(-1, (10: Byte) compareTo 7540113804746346429L)
    assertEquals(1, (10: Byte) compareTo 2)
    assertEquals(1, (10: Byte) compareTo (2:Short))
    assertEquals(1, (10: Byte) compareTo (2:Byte))
    assertEquals(1, ((10: Byte): Comparable<Byte>) compareTo (2:Byte))
    assertEquals(1, (10: Byte) compareTo 2.0)
    assertEquals(1, (10: Byte) compareTo 2.0f)
    assertEquals(-1, (10: Byte) compareTo 'A')

    assertEquals(0, 2.0 compareTo 2L)
    assertEquals(-1, 2.0 compareTo 7540113804746346429L)
    assertEquals(0, 2.0 compareTo 2)
    assertEquals(0, 2.0 compareTo (2:Short))
    assertEquals(0, 2.0 compareTo (2:Byte))
    assertEquals(0, 2.0 compareTo 2.0)
    assertEquals(0, (2.0: Comparable<Double>) compareTo 2.0)
    assertEquals(0, 2.0 compareTo 2.0f)
    assertEquals(-1, 2.0 compareTo 'A')

    assertEquals(1, 3.0f compareTo 2L)
    assertEquals(-1, 3.0f compareTo 7540113804746346429L)
    assertEquals(1, 3.0f compareTo 2)
    assertEquals(1, 3.0f compareTo (2:Short))
    assertEquals(1, 3.0f compareTo (2:Byte))
    assertEquals(1, 3.0f compareTo 2.0)
    assertEquals(1, 3.0f compareTo 2.0f)
    assertEquals(-1, 3.0f compareTo 'A')
    assertEquals(1, (3.0f: Comparable<Float>) compareTo 2.0f)

    assertEquals(1, 10L compareTo 2L)
    assertEquals(-1, 10L compareTo 7540113804746346429L)
    assertEquals(1, (10L: Comparable<Long>) compareTo 2L)
    assertEquals(1, 10L compareTo 2)
    assertEquals(1, 10L compareTo (2:Short))
    assertEquals(1, 10L compareTo (2:Byte))
    assertEquals(1, 10L compareTo 2.0)
    assertEquals(1, 10L compareTo 2.0f)
    assertEquals(-1, 10L compareTo 'A')

    assertEquals(-1, 'A' compareTo 200L)
    assertEquals(-1, 'A' compareTo 7540113804746346429L)
    assertEquals(1, 'A' compareTo 2)
    assertEquals(1, 'A' compareTo (2:Short))
    assertEquals(1, 'A' compareTo (2:Byte))
    assertEquals(1, 'A' compareTo 2.0)
    assertEquals(1, 'A' compareTo 2.0f)
    assertEquals(0, 'A' compareTo 'A')

    assertEquals(-1, 1L compareTo 2L)
    assertEquals(-1, 1L compareTo 2)
    assertEquals(-1, 1L compareTo (2:Short))
    assertEquals(-1, 1L compareTo (2:Byte))
    assertEquals(-1, 1L compareTo 2.0)
    assertEquals(-1, 1L compareTo 2.0f)

    assertEquals(0, 7540113804746346429L compareTo 7540113804746346429L)
    assertEquals(1, 7540113804746346429L compareTo 2L)
    assertEquals(0, 2L compareTo 2L)
    assertEquals(0, 2L compareTo 2)
    assertEquals(0, 2L compareTo (2:Short))
    assertEquals(0, 2L compareTo (2:Byte))
    assertEquals(0, 2L compareTo 2.0)
    assertEquals(0, 2L compareTo 2.0f)

    assertEquals(1, 10L compareTo 2L)
    assertEquals(1, 10L compareTo 2)
    assertEquals(1, 10L compareTo (2:Short))
    assertEquals(1, 10L compareTo (2:Byte))
    assertEquals(1, 10L compareTo 2.0)
    assertEquals(1, 10L compareTo 2.0f)

    assertEquals(-1, 1L compareTo 2L)
    assertEquals(-1, 1L compareTo 2)
    assertEquals(-1, 1L compareTo (2:Short))
    assertEquals(-1, 1L compareTo (2:Byte))
    assertEquals(-1, 1L compareTo 2.0)
    assertEquals(-1, 1L compareTo 2.0f)

    assertEquals(0, 2L compareTo 2L)
    assertEquals(0, 2L compareTo 2)
    assertEquals(0, 2L compareTo (2:Short))
    assertEquals(0, 2L compareTo (2:Byte))
    assertEquals(0, 2L compareTo 2.0)
    assertEquals(0, 2L compareTo 2.0f)

    assertEquals(1, id("A", 10) compareTo id("B", 5))
    assertEquals("AB", global)

    return "OK"
}