// IS_APPLICABLE: false
interface I {
    fun foo()
}

class C : I {
    override fun <caret>foo() {
    }
}