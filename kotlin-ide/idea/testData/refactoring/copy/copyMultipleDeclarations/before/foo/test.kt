package foo

class <caret>A {
    init {
        val a: A = A()
        val x: X = X()
        b()
        c
    }
}

class X {
    init {
        val a: A = A()
        val x: X = X()
        b()
        c
    }
}

fun <caret>b() {
    val a: A = A()
    val x: X = X()
    b()
    c
}

val <caret>c: Int get() {
    val a: A = A()
    val x: X = X()
    b()
    c
}