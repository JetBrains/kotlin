import utils.*

// CHECK_CONTAINS_NO_CALLS: test_0

internal fun test(a: A, y: Int): Int = a.plus(y)

fun box(): String {
    assertEquals(5, test(A(2), 3))

    return "OK"
}