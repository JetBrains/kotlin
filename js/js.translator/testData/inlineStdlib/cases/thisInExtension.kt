package foo

// CHECK_CONTAINS_NO_CALLS: testImplicitThis
// CHECK_CONTAINS_NO_CALLS: testExplicitThis

class A(var value: Int)

fun testImplicitThis(a: A, newValue: Int) {
    with (a) {
        value = newValue
    }
}

fun testExplicitThis(a: A, newValue: Int) {
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