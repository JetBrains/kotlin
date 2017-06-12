// EXPECTED_REACHABLE_NODES: 905
package foo

data class A(val bar: Int)

external class B

fun makeB(): B = js("new Object();")

fun intAgainstInt(x: Int) = when (x) {
    1 -> "a"
    2 -> "b"
    else -> "*"
}

fun intAgainstNullableInt(x: Int?) = when (x) {
    1 -> "a"
    2 -> "b"
    null -> "c"
    else -> "*"
}

fun anyAgainstInt(x: Any?) = when (x) {
    1 -> "a"
    2 -> "b"
    else -> "*"
}

fun longAgainstLong(x: Long) = when (x) {
    1L -> "a"
    2L -> "b"
    else -> "*"
}

fun anyAgainstLong(x: Any?) = when (x) {
    1L -> "a"
    2L -> "b"
    null -> "c"
    else -> "*"
}

fun anyAgainstAny(x: Any) = when (x) {
    A(1) -> "a"
    1 -> "b"
    else -> "*"
}

fun dynamicAgainstPattern(x: dynamic) = when(x) {
    1 -> "a"
    "2" -> "b"
    else -> "*"
}

fun box(): String {
    assertEquals("a", intAgainstInt(1))
    assertEquals("b", intAgainstInt(2))
    assertEquals("*", intAgainstInt(23))

    assertEquals("a", intAgainstNullableInt(1))
    assertEquals("b", intAgainstNullableInt(2))
    assertEquals("c", intAgainstNullableInt(null))
    assertEquals("*", intAgainstNullableInt(23))

    assertEquals("a", anyAgainstInt(1))
    assertEquals("b", anyAgainstInt(2))
    assertEquals("*", anyAgainstInt(A(23)))

    assertEquals("a", longAgainstLong(1))
    assertEquals("b", longAgainstLong(2))
    assertEquals("*", longAgainstLong(23))

    assertEquals("a", anyAgainstLong(1L))
    assertEquals("b", anyAgainstLong(2L))
    assertEquals("c", anyAgainstLong(null))
    assertEquals("*", anyAgainstLong(A(23)))

    assertEquals("a", anyAgainstAny(A(1)))
    assertEquals("b", anyAgainstAny(1))
    assertEquals("*", anyAgainstAny(listOf(1)))

    assertEquals("a", dynamicAgainstPattern(1))
    assertEquals("a", dynamicAgainstPattern(js("1")))
    assertEquals("b", dynamicAgainstPattern("2"))
    assertEquals("b", dynamicAgainstPattern(js("'2'")))
    assertEquals("*", dynamicAgainstPattern(js("{}")))

    return "OK"
}