package foo

// CHECK_CONTAINS_NO_CALLS: doNothingNoInline

inline fun <T> doNothing1(a: T): T {
    return a
}

inline fun <T> doNothing2(a: T, inline f: (T) -> T): T {
    return f(a)
}

fun doNothingNoInline(a: Int): Int {
    return doNothing2(a, {(x) -> doNothing1(x)})
}

fun box(): String {
    assertEquals(1, doNothingNoInline(1))
    assertEquals(12, doNothingNoInline(12))
    assertEquals(12, doNothingNoInline(12))

    return "OK"
}