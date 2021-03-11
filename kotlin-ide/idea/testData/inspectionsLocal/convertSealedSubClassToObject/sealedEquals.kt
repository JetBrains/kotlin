// PROBLEM: none

sealed class SC {
    <caret>class U : SC()

    fun foo() = 42

    override fun equals(other: Any?): Boolean {
        if (other !is SC) return false
        return foo() == other.foo()
    }
}