// EXPECTED_REACHABLE_NODES: 498
package foo

var s = ""

class A() {
    fun test(v: String) {
        s += "4"
    }
}

fun f(): String {
    s += "3"
    return ""
}

class B() {
    val a: A
        get() {
            s += "2"
            return A()
        }

    fun test() {
        s += "1"
        a.test("${if (true) f() else 4}")
        s += "5"
    }
}

fun box(): String {
    B().test()
    return if (s != "12345") "fail: $s" else "OK"
}