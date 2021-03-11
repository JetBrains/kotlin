// "Make bar suspend" "true"

suspend fun foo() {}

open class A {
    open fun bar() {
        <caret>foo()
    }
}
