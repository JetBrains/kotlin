open class A {
    open fun XYZ.foo(n: Int): Int {
        return 1
    }
}

class B : A() {
    override fun XYZ.foo(n: Int): Int {
        return 2
    }
}