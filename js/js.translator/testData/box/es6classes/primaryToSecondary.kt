// EXPECTED_REACHABLE_NODES: 1341

open class A(val x: Int, val y: Int) {
    constructor(x: Int) : this(x, x)
}

class B(x: Int) : A(x)

fun box(): String {
    val b = B(45)

    assertEquals(b.x, 45)
    assertEquals(b.y, 45)

    return "OK"
}
