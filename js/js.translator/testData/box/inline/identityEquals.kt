// EXPECTED_REACHABLE_NODES: 493
package foo

class A

inline fun compare1(a: A): Boolean {
    return a === a
}

inline fun compare2(a1: A, a2: A): Boolean {
    return a1 === a2
}

fun box(): String {
    assertEquals(true, compare1(A()), "compare1(A())")
    assertEquals(false, compare2(A(), A()), "compare2(A(), A())")

    val a = A()
    assertEquals(true, compare2(a, a), "compare2(a, a)")

    return "OK"
}