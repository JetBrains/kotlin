// GENERATOR: SINGLE_TEMPLATE
// GENERATE_SUPER_CALL
open class X {
    override fun toString() = super.toString()
}

class A : X() {<caret>
    fun foo() {

    }
}