internal open class A {
    open fun foo() {
        TODO("_root_ide_package_")
    }
}

internal open class B : A() {
    override fun foo() {
        TODO("_root_ide_package_")
    }
}

internal class C : B() {
    override fun foo() {}
}
