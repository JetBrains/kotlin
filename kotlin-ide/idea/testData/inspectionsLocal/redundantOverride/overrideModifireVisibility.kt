// PROBLEM: none

open class Foo {
    protected open fun simple() {
    }
}

class Bar : Foo() {
    public override <caret>fun simple() {
        super.simple()
    }
}

