open class A (n: Int) {
    constructor(): this(1)
}

class B: A {
    constructor(n: Int): super(n)
}

class C(): A(1)

fun test() {
    A()
    A(1)
}