// EXPECTED_REACHABLE_NODES: 493
package foo

// CHECK_FUNCTIONS_HAVE_SAME_LINES: declaredBefore declaredAfter match=(h|g)1 replace=$1

fun declaredBefore(): Int {
    val a = g() + h()
    return a
}

inline fun g(): Int {
    val a = h()
    return a
}

inline fun h(): Int {
    val a = 1
    return a
}

inline fun g1(): Int {
    val a = h1()
    return a
}

inline fun h1(): Int {
    val a = 1
    return a
}

fun declaredAfter(): Int {
    val a = g1() + h1()
    return a
}

fun box(): String {
    assertEquals(declaredBefore(), declaredAfter())

    return "OK"
}