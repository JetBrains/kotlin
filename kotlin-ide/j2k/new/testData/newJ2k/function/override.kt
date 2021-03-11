internal open class A {
    open fun a() {}
}

internal class B : A() {
    override fun a() {}
}