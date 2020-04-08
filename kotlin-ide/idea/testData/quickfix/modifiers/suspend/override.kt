// "Make bar suspend" "true"

suspend fun foo() {}

open class A {
    open suspend fun bar() {}
}

class B : A() {
    override fun bar() {
        <caret>foo()
    }
}