// PROBLEM: none

abstract class Parent {
    abstract val foo: Nothing?
}

class Child : Parent() {
    override val <caret>foo = null
}