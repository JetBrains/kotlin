open class A {
    constructor(a: Int, s: String) {

    }

    constructor(): <caret>this(1, "foo") {

    }
}

open class B: A {
    constructor(a: Int): super(a, "foo") {

    }
}

fun test() {
    A(1, "foo")
}