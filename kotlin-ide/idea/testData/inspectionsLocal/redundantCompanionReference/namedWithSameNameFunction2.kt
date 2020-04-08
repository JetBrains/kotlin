class C {
    companion object foo {
        fun foo() {}

        operator fun invoke(i: Int) {
        }
    }

    fun test() {
        <caret>foo.foo()
    }
}