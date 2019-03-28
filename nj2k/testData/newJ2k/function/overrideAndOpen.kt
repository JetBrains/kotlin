internal open class A {
    internal open fun foo() {}
}

internal open class B : A() {
    override fun foo() {}
}

internal class C : B() {
    override fun foo() {}
}