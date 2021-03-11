open class EE() {
    open fun f() = 43
}

class FF() : EE() {
    override fun f() = super<<caret>EE>@FF.f() - 1
}
/*
super<EE>@FF.f()
super<EE>@FF.f() - 1
*/