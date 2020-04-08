// "Make 'foo' internal" "true"

open class A {
    private val foo = 1
}

class B : A() {
    fun bar() {
        <caret>foo
    }
}