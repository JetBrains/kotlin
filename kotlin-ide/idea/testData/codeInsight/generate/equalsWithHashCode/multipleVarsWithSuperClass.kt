open class X {
    override fun equals(other: Any?) = super.equals(other)
    override fun hashCode() = super.hashCode()
}

class A(val n: Int, val s: String) : X() {<caret>
    val f: Float = 1.0f

    fun foo() {

    }
}