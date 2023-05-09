// EXPECTED_REACHABLE_NODES: 1424
open class C {
    private fun constructor() = "C.constructor"

    fun f(): String = constructor()
}

class D : C()

fun box(): String {
    val d = D()

    val x = d.f()
    if (x != "C.constructor") return "fail1: $x"

    if (x.asDynamic().constructor === D::class.js) return "fail2"

    return "OK"
}