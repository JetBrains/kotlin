internal open class Base {
    protected open fun foo() {
        TODO("_root_ide_package_")
    }
}

internal class Derived : Base() {
    public override fun foo() {
        super.foo()
    }
}
