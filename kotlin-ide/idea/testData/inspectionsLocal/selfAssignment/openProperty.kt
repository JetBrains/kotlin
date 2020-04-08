// PROBLEM: none

open class Test {
    open var foo = 1

    fun test() {
        foo = <caret>this.foo
    }
}