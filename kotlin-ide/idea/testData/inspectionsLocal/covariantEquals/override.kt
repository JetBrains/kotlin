// PROBLEM: 'equals' should take 'Any?' as its argument
// FIX: none
open class Foo {
    open fun equals(other: Foo?): Boolean {
        return true
    }
}

class Bar : Foo() {
    override fun <caret>equals(other: Foo?): Boolean {
        return true
    }
}
