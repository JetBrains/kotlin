package foo

class A() {
    var i = 0

    fun f() {
        for (j in 0..2) {
            foo {
                i += j
            }
        }
    }
}

fun foo(f: () -> Unit) {
    f()
}

fun box(): Boolean {
    val a = A()
    a.f()
    return a.i == 3
}
