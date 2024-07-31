// EXPECTED_REACHABLE_NODES: 1291
package foo

// CHECK_CONTAINS_NO_CALLS: doNothing1
// CHECK_CONTAINS_NO_CALLS: doNothing2
// CHECK_CONTAINS_NO_CALLS: doNothing3

internal class Inline {
    public inline fun <T> identity1 (x: T): T {
        return x
    }

    public inline fun <T> identity2 (x: T, f: (T) -> T): T {
        return f(x)
    }

    public inline fun <T> identity3 (f: () -> T): T {
        return f()
    }
}

// CHECK_BREAKS_COUNT: function=doNothing1 count=0
// CHECK_LABELS_COUNT: function=doNothing1 name=$l$block count=0
internal fun doNothing1 (inline1: Inline, a: Int): Int {
    return inline1.identity1(a)
}

// CHECK_BREAKS_COUNT: function=doNothing2 count=0
// CHECK_LABELS_COUNT: function=doNothing2 name=$l$block count=0
internal fun doNothing2 (inline2: Inline, a: Int): Int {
    return inline2.identity2(a, {it})
}

// CHECK_BREAKS_COUNT: function=doNothing3 count=0
// CHECK_LABELS_COUNT: function=doNothing3 name=$l$block count=0
internal fun doNothing3 (inline3: Inline): Int {
    return inline3.identity3({11})
}

fun box(): String {
    val inline = Inline()
    assertEquals(1, doNothing1(inline, 1))
    assertEquals(2, doNothing1(inline, 2))
    assertEquals(3, doNothing2(inline, 3))
    assertEquals(4, doNothing2(inline, 4))
    assertEquals(11, doNothing3(inline))

    return "OK"
}