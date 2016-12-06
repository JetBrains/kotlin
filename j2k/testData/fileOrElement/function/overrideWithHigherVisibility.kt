internal open class Base {
    protected open fun foo() {}
}

internal class Derived : Base() {
    public override fun foo() {
        super.foo()
    }
}
