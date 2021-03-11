open class EE() {
    open fun f() = 43
}

class FF() : EE() {
    override fun f() = <caret>super.f() - 1
}
/*
super.f()
super.f() - 1
*/