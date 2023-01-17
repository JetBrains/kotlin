// EXPECTED_REACHABLE_NODES: 1359

open class A(val x: Int) {
    constructor(): this(100)
}

class PrimaryToPrimary(val p: Int): A(p * p)
class PrimaryToSecondary(val p: Int): A()

class SecondaryToPrimary : A {
    constructor() : super(8)
}
class SecondaryToSecondary: A {
    constructor() : super()
}

fun box(): String {
    val ptp = PrimaryToPrimary(5)
    assertEquals(ptp.p, 5)
    assertEquals(ptp.x, 25)

    val pts = PrimaryToSecondary(9)
    assertEquals(pts.p, 9)
    assertEquals(pts.x, 100)

    val stp = SecondaryToPrimary()
    assertEquals(stp.x, 8)

    val sts = SecondaryToSecondary()
    assertEquals(sts.x, 100)

    return "OK"
}
