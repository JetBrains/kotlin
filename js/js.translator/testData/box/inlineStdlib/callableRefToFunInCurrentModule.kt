package foo

// CHECK_NOT_CALLED_IN_SCOPE: scope=test function=even
// CHECK_NOT_CALLED_IN_SCOPE: scope=test function=filter_azvtw4$

internal inline fun even(x: Int) = x % 2 == 0

internal fun test(a: List<Int>) = a.filter(::even)

fun box(): String {
    assertEquals(listOf(2, 4), test(listOf(1, 2, 3, 4)))

    return "OK"
}