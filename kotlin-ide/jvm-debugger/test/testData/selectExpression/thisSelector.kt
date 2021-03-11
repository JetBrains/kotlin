class MyClass {
    fun test() {
        this.<caret>test()
    }
}

// EXPECTED: this.test()