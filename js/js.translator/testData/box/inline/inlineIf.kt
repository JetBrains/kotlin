// EXPECTED_REACHABLE_NODES: 494
package foo

// CHECK_CONTAINS_NO_CALLS: testIf1
// CHECK_CONTAINS_NO_CALLS: testIf2
// CHECK_CONTAINS_NO_CALLS: testIf3

inline fun if1(f: (Int) -> Int, a: Int, b: Int, c: Int): Int {
    val result = f(a)

    if (result == b) {
        return f(a)
    }

    return f(c)
}

inline fun if2(f: (Int) -> Int, a: Int, b: Int, c: Int): Int {
    if (f(a) == b) {
        return f(a)
    }

    return f(c)
}

inline fun if3(f: (Int) -> Int, a: Int, b: Int, c: Int): Int {
    if (f(a) == b) {
        return f(a)
    } else {
        return f(c)
    }
}

fun testIf1(): String {
    val test1 = if1({it}, 1, 2, 3)
    if (test1 != 3) return "testIf1: test1 fail"

    val test2 = if1({it}, 2, 2, 3)
    if (test2 != 2) return "testIf1: test 2 fail"

    return "OK"
}

fun testIf2(): String {
    val test1 = if2({it}, 1, 2, 3)
    if (test1 != 3) return "testIf2: test1 fail"

    val test2 = if2({it}, 2, 2, 3)
    if (test2 != 2) return "testIf2: test 2 fail"

    return "OK"
}

fun testIf3(): String {
    val test1 = if3({it}, 1, 2, 3)
    if (test1 != 3) return "testIf3: test1 fail"

    val test2 = if3({it}, 2, 2, 3)
    if (test2 != 2) return "testIf3: test 2 fail"

    return "OK"
}

fun box(): String {
    assertEquals("OK", testIf1())
    assertEquals("OK", testIf2())
    assertEquals("OK", testIf3())

    return "OK"
}