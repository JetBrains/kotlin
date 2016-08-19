@Deprecated("")
interface Intf {
    fun a()
}

open class A {
    open fun a() {}
}

class B : A(), Intf {
    override fun a() {}
}