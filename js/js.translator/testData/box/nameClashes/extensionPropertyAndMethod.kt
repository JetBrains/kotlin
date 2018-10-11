// EXPECTED_REACHABLE_NODES: 1286
package foo

class A

fun A.bar() = 23

val A.bar: Int
    get() = 42

fun box(): String {
    assertEquals(23, A().bar())
    assertEquals(42, A().bar)

    return "OK"
}
