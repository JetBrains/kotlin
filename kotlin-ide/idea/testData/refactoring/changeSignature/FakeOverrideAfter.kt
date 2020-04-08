interface A {
    fun f(i: Int) {}
}

interface B {
    fun f(i: Int) {}
}

interface C : A, B {

}

fun usage(c: C) {
    c.f()
}