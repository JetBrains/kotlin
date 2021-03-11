open class A {
    open fun foo() {

    }
}

class B : A() {
    override fun foo() {
        <selection>super</selection>.foo()
    }
}