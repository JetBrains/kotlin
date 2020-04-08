internal open class Base {
    companion object {
        val CONSTANT = 10
    }
}

internal class Derived : Base() {
    fun foo() {}
}