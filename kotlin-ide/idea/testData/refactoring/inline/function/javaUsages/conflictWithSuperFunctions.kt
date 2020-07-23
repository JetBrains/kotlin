// ERROR: Inlined function overrides function Adw1.a\nInlined function overrides function Asswe.a\nInlined function overrides function D.a

open class A {
    open fun a() = Unit
    fun ds() = Unit
}

open class B: A() {
    override fun a() = Unit
    open fun c() = a()
}

open class D: B() {
    override fun a() = Unit
    override fun c() = a()
}

interface Asswe {
    fun a()
}

class M: D(), Adw1, Asswe {
    override fun <caret>a() = Unit
    override fun c() = a()
}