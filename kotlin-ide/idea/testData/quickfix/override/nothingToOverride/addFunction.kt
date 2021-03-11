// "Add 'open fun f()' to 'A'" "true"
open class A {
}
class B : A() {
    <caret>override fun f() {}
}