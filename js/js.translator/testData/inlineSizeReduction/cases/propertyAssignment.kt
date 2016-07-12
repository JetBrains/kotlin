package foo

// CHECK_CONTAINS_NO_CALLS: test
// CHECK_VARS_COUNT: function=test count=0

object SumHolder {
    var sum = 0
}

internal inline fun sum(x: Int, y: Int): Int {
    if (x == 0 || y == 0) return 0

    return x + y
}

internal fun test(x: Int, y: Int) {
    SumHolder.sum = sum(x, y)
}

fun box(): String {
    test(1, 2)
    assertEquals(3, SumHolder.sum)

    return "OK"
}