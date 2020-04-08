// PROBLEM: none
open class A {
    open fun getFoo(): String? = null
}

class B : A() {
    private val foo = ""

    <caret>override fun getFoo(): String? = super.getFoo()
}