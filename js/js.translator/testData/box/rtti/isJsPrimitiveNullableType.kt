// EXPECTED_REACHABLE_NODES: 1291
package foo

val NUMBER = 1
val STRING = 2
val BOOLEAN = 3
val OBJECT = 4
val FUNCTION = 5
val FUNCTION0 = FUNCTION // right now we can't distinguish functions with different arity

fun testNullable(a: Any?, actualType: Int) {
    assertEquals(a == null || actualType == NUMBER, a is Int?, "$a is Int?")
    assertEquals(a == null || actualType == NUMBER, a is Number?, "$a is Number?")
    assertEquals(a == null || actualType == NUMBER, a is Double?, "$a is Double?")
    assertEquals(a == null || actualType == BOOLEAN, a is Boolean?, "$a is Boolean?")
    assertEquals(a == null || actualType == STRING, a is String?, "$a is String?")
    assertEquals(a == null || actualType == FUNCTION0, a is Function0<*>?, "$a is Function0?")
    assertEquals(a == null || actualType == FUNCTION || actualType == FUNCTION0, a is Function<*>?, "$a is Function?")
}

fun box(): String {
    testNullable(1, NUMBER)

    testNullable(12.3, NUMBER)
    testNullable(12.3f, NUMBER)

    testNullable("text", STRING)

    testNullable(true, BOOLEAN)
    testNullable(false, BOOLEAN)

    testNullable(object {}, OBJECT)
    testNullable({}, FUNCTION0)

    testNullable({}, FUNCTION)
    testNullable({a: Any -> }, FUNCTION)

    testNullable(null, NUMBER)
    testNullable(null, STRING)
    testNullable(null, BOOLEAN)
    testNullable(null, OBJECT)
    testNullable(null, FUNCTION0)
    testNullable(null, FUNCTION)

    return "OK"
}