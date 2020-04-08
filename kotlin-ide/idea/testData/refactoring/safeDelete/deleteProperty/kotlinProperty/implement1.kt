interface A {
    val foo: String
}

class B: A {
    override val <caret>foo: String
        get() {
            return "B"
        }
}