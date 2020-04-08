class A(a: Int, val a1: Int) {

    <caret>constructor(b: Int): this(b, b) {}

    val c = 1
}