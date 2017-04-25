// EXPECTED_REACHABLE_NODES: 501
// See KT-11823
package foo

class Outer(val x: Int) {
    inner class Inner() {
        fun foo(): Int {
            class A {
                fun bar() = x
            }
            return A().bar()
        }
    }
}

fun box(): String {
    assertEquals(23, Outer(23).Inner().foo())
    return "OK"
}