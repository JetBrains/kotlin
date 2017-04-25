// EXPECTED_REACHABLE_NODES: 491
package foo

class A {
    var a: Int = 0
        get() = field + 1
        set(arg) {
            field = arg
        }
}

fun box(): String {
    val a = A()
    a.a = 1
    if (a.a != 2) return "A().a != 2, it: ${a.a}"
    return "OK"
}