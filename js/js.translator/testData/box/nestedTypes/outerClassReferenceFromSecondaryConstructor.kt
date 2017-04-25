// EXPECTED_REACHABLE_NODES: 501
package foo

class A(val x: Int) {
    inner class B() {
        inner class C() {
            var result = 0

            constructor(y: Boolean) : this() {
                result = x
            }
        }
    }
}

fun box(): String {
    assertEquals(23, A(23).B().C(true).result)

    return "OK"
}