open class A {
    open val foo: String
        get() {
            return "A"
        }
}

interface Z {
    val foo: String
}

class B: A(), Z {
    override val <caret>foo: String
        get() {
            return "B"
        }
}