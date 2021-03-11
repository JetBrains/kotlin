open class B {
    constructor(): this("") {

    }

    constructor(s: String) {

    }
}

open class A : B {
    constructor(a: Int) : super("") {

    }
}

class C : B("") {

}

fun test() {
    B("")
}