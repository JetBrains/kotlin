abstract class Parent {
    abstract val foo: Int?
}

class Child : Parent() {
    override val <caret>foo = null
}