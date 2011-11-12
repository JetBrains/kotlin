namespace foo

open class C {
    open fun f(): Any = "C f"
}

class D() : C {
    override fun f(): String = "D f"
}

fun box(): Boolean {
    val d : C = D()
    if(d.f() != "D f") return false
    return true
}