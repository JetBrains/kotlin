class A {
    companion object {
        class B {

        }

        val <caret>foo: Int = 1
    }

    fun bar() {
        foo + 1
    }
}