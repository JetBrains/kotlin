// EXPECTED_REACHABLE_NODES: 1344

open class A(var value: Int) {
    init {
        value *= 2
    }
}

class B : A {
    init {
        value /= 6
    }

    constructor(x: Int) : super(x) {
        value *= 18
    }

    constructor() : this(18) {
        value *= 12
    }
}

fun box(): String {
    val bs1 = B(15)
    assertEquals(90, bs1.value)

    val bs2 = B()
    assertEquals(72 * 18, bs2.value)

    return "OK"
}
