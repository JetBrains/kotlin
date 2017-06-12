// EXPECTED_REACHABLE_NODES: 530
package foo

enum class Type {
    NUMBER,
    STRING,
    BOOLEAN,
    OBJECT
}

fun test(a: dynamic, actualType: Type) {
    assertEquals(actualType == Type.NUMBER, a is Int, "$a is Int")
    assertEquals(actualType == Type.NUMBER, a is Number, "$a is Number")
    assertEquals(actualType == Type.NUMBER, a is Double, "$a is Double")
    assertEquals(actualType == Type.BOOLEAN, a is Boolean, "$a is Boolean")
    assertEquals(actualType == Type.STRING, a is String, "$a is String")
}

fun box(): String {
    test(1, Type.NUMBER)

    test(12.3, Type.NUMBER)
    test(12.3f, Type.NUMBER)

    test("text", Type.STRING)

    test(true, Type.BOOLEAN)
    test(false, Type.BOOLEAN)

    test(object {}, Type.OBJECT)

    return "OK"
}