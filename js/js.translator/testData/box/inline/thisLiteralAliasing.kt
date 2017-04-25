// EXPECTED_REACHABLE_NODES: 500
package foo

class A() {
    public var param: Int = 0

    inline public fun setParam(value: Int) {
        val b = B(value)
        b.setParam(this)
    }
}

class B(val value: Int) {
    inline fun setParam(a: A) {
        a.param = this.value
    }
}

public fun box(): String {
    val a = A()
    a.setParam(10)
    assertEquals(10, a.param)

    return "OK"
}