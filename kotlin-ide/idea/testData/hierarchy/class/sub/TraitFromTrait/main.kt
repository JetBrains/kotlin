interface <caret>A

interface B: A

class MyClass(a: A = run { interface X: A; object: A {} }) {
    inner interface G: A {}

    init {
        interface C: A
    }

    fun foo(a: A = run { interface X: A; object: A {} }) {
        interface D: A

        val t = object {
            inner interface F: A
        }
    }
}

val bar: Int
    get() {
        interface E: A

        return 0
    }