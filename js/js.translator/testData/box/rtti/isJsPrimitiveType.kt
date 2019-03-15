// EXPECTED_REACHABLE_NODES: 1291
package foo

val NUMBER = 1
val STRING = 2
val BOOLEAN = 3
val OBJECT = 4
val FUNCTION = 5

fun test(a: Any, actualType: Int) {
    assertEquals(actualType == NUMBER, a is Int, "$a is Int")
    assertEquals(actualType == NUMBER, a is Number, "$a is Number")
    assertEquals(actualType == NUMBER, a is Double, "$a is Double")
    assertEquals(actualType == BOOLEAN, a is Boolean, "$a is Boolean")
    assertEquals(actualType == STRING, a is String, "$a is String")
    assertEquals(actualType == FUNCTION, a is Function0<*>, "$a is Function")
}

fun box(): String {
    test(1, NUMBER)

    test(12.3, NUMBER)
    test(12.3f, NUMBER)

    test("text", STRING)

    test(true, BOOLEAN)
    test(false, BOOLEAN)

    test(object {}, OBJECT)

    test({}, FUNCTION)

    return "OK"
}