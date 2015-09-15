internal open class Base {
    companion object {
        val CONSTANT: Int = 10
    }
}

internal class Derived : Base() {
    internal fun foo() {
    }
}
