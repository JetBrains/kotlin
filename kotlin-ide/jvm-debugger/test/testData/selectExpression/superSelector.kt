class Derived: Base() {
    fun test() {
        super.<caret>test()
    }
}

open class Base {
    fun test() {}
}

// EXPECTED: super.test()