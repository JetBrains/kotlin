class Derived: Base() {
    fun test() {
        <caret>super.test()
    }
}

open class Base {
    fun test() {}
}

// EXPECTED: null