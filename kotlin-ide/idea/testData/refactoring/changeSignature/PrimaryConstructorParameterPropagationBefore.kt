open class <caret>A() {
    constructor(b: Boolean): this()
}

open class B: A {
    constructor()
    constructor(b: Boolean): super()
}

open class C(b: Boolean): A()

fun test() {
    A()
    A(false)
    B()
    B(false)
    C(false)
    J()
    J(false)
}