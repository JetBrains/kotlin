// EXPECTED_REACHABLE_NODES: 497
package foo

class A

class B

val A.foo: Int
    get() = 32

val B.foo: Int
    get() = 42

fun box(): String {
    assertEquals(32, A().foo)
    assertEquals(42, B().foo)

    return "OK"
}