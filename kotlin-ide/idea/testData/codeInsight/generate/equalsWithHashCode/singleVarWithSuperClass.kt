open class X {
    override fun equals(other: Any?) = super.equals(other)
    override fun hashCode() = super.hashCode()
}

class A(val n: Int) : X() {<caret>
    fun foo() {

    }
}