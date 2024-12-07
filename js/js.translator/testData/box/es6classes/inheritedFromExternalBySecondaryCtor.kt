open external class E(x: Int, y: Int) {
    val t: Int = definedExternally
}

open class A(i: Int, j: Int) : E(i, j)

class B : A {
    constructor() : super(3, 4)
}

fun box(): String {
    val b = B()

    assertEquals(7, b.t)

    return "OK"
}
