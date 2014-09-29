package foo

// CHECK_CONTAINS_NO_CALLS: identity
// CHECK_CONTAINS_NO_CALLS: sumNoInline

inline fun sum(a: Int, b: Int = 0): Int {
    return a + b
}

fun identity(a: Int): Int {
    return sum(a)
}

fun sumNoInline(a: Int, b: Int): Int {
    return sum(a, b)
}

fun box(): String {
    assertEquals(1, identity(1))
    assertEquals(2, identity(2))
    assertEquals(3, sumNoInline(1, 2))
    assertEquals(5, sumNoInline(2, 3))

    return "OK"
}