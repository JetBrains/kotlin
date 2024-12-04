open external class E(x: Int, y: Int) {
    val t: Int = definedExternally
}

open class A(i: Int, j: Int) : E(i, j)

class B(val ok: String) : A(2, 3)

fun box(): String {
    val b = B("OK")

    assertEquals(5, b.t)

    return b.ok
}
