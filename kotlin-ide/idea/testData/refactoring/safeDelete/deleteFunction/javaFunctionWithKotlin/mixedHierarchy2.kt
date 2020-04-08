interface B: A {
    public override fun foo() {
        throw UnsupportedOperationException()
    }
}

class D: C() {
    public override fun foo() {
        throw UnsupportedOperationException()
    }
}