open class <caret>B() {
    constructor(a: Int): this() {

    }
}

open class A: B {
    constructor(a: Int): super() {

    }
}