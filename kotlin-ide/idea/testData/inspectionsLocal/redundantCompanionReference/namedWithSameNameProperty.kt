// PROBLEM: none
class A {
    companion object foo {
        val foo = 1
    }

    fun test() {
        <caret>foo.foo
    }
}