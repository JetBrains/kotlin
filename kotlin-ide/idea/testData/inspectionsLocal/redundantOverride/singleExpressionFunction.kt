open class Foo {
    open fun singleExpression() {
    }
}

class Bar : Foo() {
    override <caret>fun singleExpression() = super.singleExpression()
}
