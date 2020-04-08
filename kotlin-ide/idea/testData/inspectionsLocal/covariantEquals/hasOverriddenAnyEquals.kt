// PROBLEM: none
class Foo {
    fun <caret>equals(other: Foo?): Boolean {
        return true
    }

    override fun equals(other: Any?): Boolean {
        return true
    }
}