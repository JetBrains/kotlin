import utils.*

// CHECK_CONTAINS_NO_CALLS: test_0

internal fun test(x: Int, y: Int): Int = sum(x, y)

fun box(): String {
    assertEquals(3, test(1, 2))
    assertEquals(5, test(2, 3))

    return "OK"
}