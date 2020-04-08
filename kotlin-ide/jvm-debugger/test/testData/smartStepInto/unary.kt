class A {
    operator fun unaryPlus() {}
    operator fun unaryMinus() {}
}

fun foo() {
    <caret>+A() || -A()
}
// EXISTS: unaryPlus(), unaryMinus()