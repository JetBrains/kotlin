// EXPECTED_REACHABLE_NODES: 496
package foo

class A() {
    var i = 0

    fun boo() = 1

    fun f() {
        for (j in 0..2) {
            foo {
                i += j + boo()
            }
        }
    }
}

fun A.bar(): Int {
    for (u in 4..7) {
        foo {
            i += u + boo()
        }
    }

    return i
}

fun foo(f: () -> Unit) {
    f()
}

fun box(): String {
    val a = A()
    a.f()
    if (a.i != 6) return "fail1: ${a.i}"
    if (a.bar() != 32) return "fail2: ${a.bar()}"

    return "OK"
}
