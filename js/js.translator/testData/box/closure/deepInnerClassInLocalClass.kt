// EXPECTED_REACHABLE_NODES: 515
package foo

class A() {
    fun test(): Int {
        open class B(open val x: Int) {
            inner class C(x: Int) : B(x * 10) {
                inner class D() {
                    var baz: () -> Int = { 0 }
                    constructor(b: Boolean) : this() {
                        baz = { x + this@B.x }
                    }
                    fun bar() = { 100 * (x + this@B.x) }
                }
            }
        }
        return B(3).C(2).D().bar()() + B(5).C(4).D(true).baz()
    }
}

fun box(): String {
    assertEquals(2345, A().test())
    return "OK"
}