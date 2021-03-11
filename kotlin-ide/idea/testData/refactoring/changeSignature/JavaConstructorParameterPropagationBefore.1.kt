open class C: A() {

}

open class D: A {
    constructor(): super()
    constructor(b: Boolean)
}

fun test() {
    A()
    A(false)
    B()
    B(false)
    C()
    D()
    D(false)
}