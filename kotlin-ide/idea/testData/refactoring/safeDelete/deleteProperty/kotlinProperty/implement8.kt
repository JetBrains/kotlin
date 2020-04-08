interface A {
    var <caret>foo: String
}

interface Z {
    var foo: String
}

class B: A, Z {
    override val foo: String
        get() {
            return "B"
        }
        set(value: String) {

        }
}