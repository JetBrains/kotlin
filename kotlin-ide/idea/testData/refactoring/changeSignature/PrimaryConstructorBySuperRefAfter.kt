open class A(s: String) {
    constructor(a: Int) : this("foo") {

    }
}

open class B: A {
    constructor(a: Int) : super("foo") {

    }
}

open class C: A {
    constructor(a: Int) : super("foo") {

    }
}

fun test() {
    A("foo")
}