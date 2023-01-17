// EXPECTED_REACHABLE_NODES: 1340

open class A(val x: Int)

class B(val p: Int, val q: Int): A(p + q)

fun box(): String {
    val b = B(2, 3)
    assertEquals(b.p, 2)
    assertEquals(b.q, 3)
    assertEquals(b.x, 5)

    return "OK"
}
