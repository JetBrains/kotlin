open class A {
    open var <caret>foo: String
        get() {
            return "A"
        }
        set(value: String) {
            println()
        }
}

class B: A() {
    override var foo: String
        get() {
            return "B"
        }
        set(value: String) {
            println()
        }
}