package test

open class A<T> {
    open fun foo() {
    }
}

class B<T> : A<T>() {
    override fun foo() {
        super.foo()
    }
}