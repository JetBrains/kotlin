// EXPECTED_REACHABLE_NODES: 506
package foo

// CHECK_NOT_CALLED: inline1
// CHECK_NOT_CALLED: inline2
// CHECK_NOT_CALLED: inline3

inline fun inline1(a: Int): Int {
    return a
}

inline fun inline2(a: Int): Int {
    val a1 = inline1(a)
    if (a1 == 0) return 0
    return a1 + inline1(a)
}

inline fun inline3(a: Int): Int {
    val i = inline2(a)
    val i1 = inline1(a) * i
    if (i == i1) return 0
    return i1
}

val r1 = inline3(1)
val r3 = inline3(3)
val r4 = inline3(4)

fun box(): String {
    assertEquals(0, r1)
    assertEquals(18, r3)
    assertEquals(32, r4)

    return "OK"
}
