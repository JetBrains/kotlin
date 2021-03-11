open class A {
    open fun plus(other: A): A = A()
}

class B : A() {
    override fun <caret>plus(other: A): A {
        return super.plus(other)
    }
}
