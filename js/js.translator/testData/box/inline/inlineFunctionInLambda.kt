// EXPECTED_REACHABLE_NODES: 492
package foo

// CHECK_CONTAINS_NO_CALLS: doNothingNoInline

internal inline fun <T> doNothing1(a: T): T {
    return a
}

internal inline fun <T> doNothing2(a: T, f: (T) -> T): T {
    return f(a)
}

internal fun doNothingNoInline(a: Int): Int {
    return doNothing2(a, { x -> doNothing1(x)})
}

fun box(): String {
    assertEquals(1, doNothingNoInline(1))
    assertEquals(12, doNothingNoInline(12))
    assertEquals(12, doNothingNoInline(12))

    return "OK"
}