// IS_APPLICABLE: false

interface A {
    val foo: Int
}

class B: A {
    override val <caret>foo: Int = 1
}