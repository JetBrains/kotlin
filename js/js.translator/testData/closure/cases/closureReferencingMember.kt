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

fun box(): Boolean {
    val a = A()
    a.f()
    return a.i == 6 && a.bar() == 32
}
