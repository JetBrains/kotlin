// GENERATOR: MULTIPLE_TEMPLATES
// GENERATE_SUPER_CALL
open class X {
    override fun toString() = super.toString()
}

class A(val n: Int, val s: String) : X() {<caret>
    val f: Float = 1.0f

    fun foo() {

    }
}