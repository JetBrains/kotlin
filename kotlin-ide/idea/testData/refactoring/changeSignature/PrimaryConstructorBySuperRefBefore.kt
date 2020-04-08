open class A() {
    constructor(a: Int) : this() {

    }
}

open class B: A {
    constructor(a: Int) : <caret>super() {

    }
}

open class C: A {
    constructor(a: Int) {

    }
}

fun test() {
    A()
}