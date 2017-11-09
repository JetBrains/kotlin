internal open class A {
    internal open fun a() {}
}

internal class B : A() {
    override fun a() {}
}