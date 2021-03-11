// PROBLEM: none
open class Foo {
    open fun arguments(arg1: Int, arg2: Int) {
    }
}

class Bar : Foo() {
    override <caret>fun arguments(arg1: Int, arg2: Int) {
        super.arguments(arg2, arg1)
    }
}
