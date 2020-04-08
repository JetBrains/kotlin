// IS_APPLICABLE: false

interface A {
    fun foo()
}

class B: A {
    override fun <caret>foo() {

    }
}