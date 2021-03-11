class MyClass {
    fun Int.test() {
        <caret>this@MyClass
    }
}

// EXPECTED: this@MyClass