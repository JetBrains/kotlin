// EXPECTED_REACHABLE_NODES: 1283
package foo

// CHECK_CONTAINS_NO_CALLS: doNothingNoInline

internal inline fun <T> doNothing1(a: T): T {
    return a
}

internal inline fun <T> doNothing2(a: T, f: (T) -> T): T {
    return f(a)
}

// CHECK_BREAKS_COUNT: function=doNothingNoInline count=0
// CHECK_LABELS_COUNT: function=doNothingNoInline name=$l$block count=0
internal fun doNothingNoInline(a: Int): Int {
    return doNothing2(a, { x -> doNothing1(x)})
}

fun box(): String {
    assertEquals(1, doNothingNoInline(1))
    assertEquals(12, doNothingNoInline(12))
    assertEquals(12, doNothingNoInline(12))

    return "OK"
}