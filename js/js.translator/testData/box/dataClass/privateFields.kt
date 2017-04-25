// EXPECTED_REACHABLE_NODES: 953
package foo

data class A(private val x: Int) {
    val y: Int
        get() = x
}

fun box(): String {
    val a = A(23)

    assertEquals("A(x=23)", a.toString())
    assertEquals(23, a.copy().y)
    assertEquals(42, a.copy(42).y)

    assertEquals(A(23), A(23))
    assertNotEquals(A(42), A(23))

    val map = mapOf(A(23) to "*", A(42) to "@")
    assertEquals("*", map[A(23)])
    assertEquals("@", map[A(42)])
    assertEquals(null, map[A(93)])

    return "OK"
}