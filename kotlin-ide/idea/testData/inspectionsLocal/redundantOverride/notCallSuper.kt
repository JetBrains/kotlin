// PROBLEM: none

open class Foo {
    open fun simple() {
    }
}

class Bar : Foo() {
    override <caret>fun simple() {
        1 + 1;
    }
}
