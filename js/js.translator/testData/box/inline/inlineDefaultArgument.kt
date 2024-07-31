// EXPECTED_REACHABLE_NODES: 1284
package foo

// CHECK_CONTAINS_NO_CALLS: identity
// CHECK_CONTAINS_NO_CALLS: sumNoInline

internal inline fun sum(a: Int, b: Int = 0): Int {
    return a + b
}

// CHECK_BREAKS_COUNT: function=identity count=0
// CHECK_LABELS_COUNT: function=identity name=$l$block count=0
internal fun identity(a: Int): Int {
    return sum(a)
}

// CHECK_BREAKS_COUNT: function=sumNoInline count=0
// CHECK_LABELS_COUNT: function=sumNoInline name=$l$block count=0
internal fun sumNoInline(a: Int, b: Int): Int {
    return sum(a, b)
}

fun box(): String {
    assertEquals(1, identity(1))
    assertEquals(2, identity(2))
    assertEquals(3, sumNoInline(1, 2))
    assertEquals(5, sumNoInline(2, 3))

    return "OK"
}