class A
fun foo() {
    interface Z: A {}
    fun bar() {
        class <caret>O2: Z {}
    }
}