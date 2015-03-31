open class Base {
    companion object {
        public val CONSTANT: Int = 10
    }
}

class Derived : Base() {
    fun foo() {
    }
}
