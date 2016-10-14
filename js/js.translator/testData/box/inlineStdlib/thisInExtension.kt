package foo

// CHECK_CONTAINS_NO_CALLS: testImplicitThis_0
// CHECK_CONTAINS_NO_CALLS: testExplicitThis_0

internal class A(var value: Int)

internal fun testImplicitThis(a: A, newValue: Int) {
    with (a) {
        value = newValue
    }
}

internal fun testExplicitThis(a: A, newValue: Int) {
    with (a) {
        this.value = newValue
    }
}

fun box(): String {
    val a = A(0)
    assertEquals(0, a.value)

    testImplicitThis(a, 10)
    assertEquals(10, a.value)

    testExplicitThis(a, 20)
    assertEquals(20, a.value)

    return "OK"
}