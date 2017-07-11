// EXPECTED_REACHABLE_NODES: 997
package foo

class A {
    var x = 23
}

inline fun bar(value: Int, a: A): Int {
    a.x = 42
    return value
}

fun box(): String {
    val a = A()
    assertEquals(23, bar(a.x, a))
    return "OK"
}