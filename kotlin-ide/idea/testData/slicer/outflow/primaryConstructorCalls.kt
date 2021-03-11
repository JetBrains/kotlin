// FLOW: OUT

open class A<caret>(n: Int) {
    constructor() : this(1)
}

class B : A(1)

class C : A {
    constructor() : super(1)
}

fun test() {
    val x = A(1)
}