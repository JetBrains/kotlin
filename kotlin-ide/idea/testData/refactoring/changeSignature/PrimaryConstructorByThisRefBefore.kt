open class A() {
    constructor(a: Int) : <caret>this() {

    }
}

open class B: A {
    constructor(a: Int) : super() {

    }
}

open class C: A {
    constructor(a: Int) {

    }
}

fun test() {
    A()
}