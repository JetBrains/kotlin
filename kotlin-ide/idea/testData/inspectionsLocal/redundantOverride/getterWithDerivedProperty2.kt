// PROBLEM: none
open class A {
    open fun isFoo(): Boolean = true
}

class B : A() {
    private val isFoo: Boolean = false

    <caret>override fun isFoo(): Boolean = super.isFoo()
}