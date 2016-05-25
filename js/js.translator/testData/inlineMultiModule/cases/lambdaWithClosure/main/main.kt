import utils.*

// CHECK_CONTAINS_NO_CALLS: test_0

internal fun test(x: Int, y: Int): Int = apply(x) { it + y }

fun box(): String {
    assertEquals(3, test(1, 2))

    return "OK"
}