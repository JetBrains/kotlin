class A(val x: String) {

    class C {<caret>}

    val foo = 1

    constructor(x: String, y: Int) : this(x) {
    }
}