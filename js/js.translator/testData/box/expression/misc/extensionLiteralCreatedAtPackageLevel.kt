// EXPECTED_REACHABLE_NODES: 495
package foo

class A() {
    fun foo() = 1
    fun a(f: A.() -> Int): Int {
        return f()
    }
}

var d = 0

val p: A.() -> Int = {
    d = foo()
    d++
}

val c = A().a(p)

fun box(): String {
    if (c != 1) return "fail: $c"
    return "OK"
}
