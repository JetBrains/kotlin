// "Add 'open fun f(): Int' to 'A'" "true"
// WITH_RUNTIME
open class A {
}
class B : A() {
    <caret>override fun f(): Int = 5
}