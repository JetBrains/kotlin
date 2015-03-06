package foo

val Int.abs: Int
    get() = if (this >= 0) this else -this

fun test(xs: List<Int>): List<Int> =
        xs.sortBy(comparator {(a, b) -> a.abs.compareTo(b.abs) })

fun box(): String {
    assertEquals(listOf(1, -2, 3, -4), test(listOf(-2, 1, -4, 3)))

    return "OK"
}