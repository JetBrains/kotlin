// PROBLEM: none
class Bar : Foo() {
    <caret>override fun foo() {
        super.foo()
    }
}