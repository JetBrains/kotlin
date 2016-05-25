import utils.*

// CHECK_CONTAINS_NO_CALLS: test_0

internal fun test(x: Int, y: Int): Int = apply(x) { it + 1 } * y

fun box(): String {
    assertEquals(6, test(1, 3))

    return "OK"
}