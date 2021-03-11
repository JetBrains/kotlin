open class C(n: Int) : A(n) {

}

open class D: A {
    constructor(n: Int): super(n)
    constructor(b: Boolean, n: Int) : super(n)
}

fun test(n: Int) {
    A(n)
    A(false, n)
    B(n)
    B(false, n)
    C(n)
    D(n)
    D(false, n)
}