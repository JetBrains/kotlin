package foo

var global: String = ""

fun id(s: String, value: Int): Int {
    global += s
    return value
}

fun box(): String {

    assertEquals(-1, 1.compareTo(2))

    assertEquals(-1, 1 compareTo 2)
    assertEquals(-1, 1 compareTo (2:Short))
    assertEquals(-1, 1 compareTo (2:Byte))
    assertEquals(-1, 1 compareTo 2.0)
    assertEquals(-1, 1 compareTo 2.0f)

    assertEquals(1, 10 compareTo 2)
    assertEquals(1, 10 compareTo (2:Short))
    assertEquals(1, 10 compareTo (2:Byte))
    assertEquals(1, 10 compareTo 2.0)
    assertEquals(1, 10 compareTo 2.0f)

    assertEquals(0, 2 compareTo 2)
    assertEquals(0, 2 compareTo (2:Short))
    assertEquals(0, 2 compareTo (2:Byte))
    assertEquals(0, 2 compareTo 2.0)
    assertEquals(0, 2 compareTo 2.0f)

    assertEquals(-1, (1: Short) compareTo 2)
    assertEquals(-1, (1: Short) compareTo (2:Short))
    assertEquals(-1, (1: Short) compareTo (2:Byte))
    assertEquals(-1, (1: Short) compareTo 2.0)
    assertEquals(-1, (1: Short) compareTo 2.0f)

    assertEquals(1, (10: Byte) compareTo 2)
    assertEquals(1, (10: Byte) compareTo (2:Short))
    assertEquals(1, (10: Byte) compareTo (2:Byte))
    assertEquals(1, (10: Byte) compareTo 2.0)
    assertEquals(1, (10: Byte) compareTo 2.0f)

    assertEquals(0, 2.0 compareTo 2)
    assertEquals(0, 2.0 compareTo (2:Short))
    assertEquals(0, 2.0 compareTo (2:Byte))
    assertEquals(0, 2.0 compareTo 2.0)
    assertEquals(0, 2.0 compareTo 2.0f)

    assertEquals(1, 3.0f compareTo 2)
    assertEquals(1, 3.0f compareTo (2:Short))
    assertEquals(1, 3.0f compareTo (2:Byte))
    assertEquals(1, 3.0f compareTo 2.0)
    assertEquals(1, 3.0f compareTo 2.0f)

    assertEquals(1, id("A", 10) compareTo id("B", 5))
    assertEquals("AB", global)

    return "OK"
}