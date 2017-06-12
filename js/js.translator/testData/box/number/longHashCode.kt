// EXPECTED_REACHABLE_NODES: 491
package foo

fun box(): String {
    var l1: Long = 0x12344478935690
    var l2: Long = 0x12344478935698
    var diff: Long = l2 - l1
    l1 += (diff / 2)
    l2 -= (diff / 2)

    assertEquals(l1, l2, "When L1 == L2")
    assertEquals(l1.hashCode(), l2.hashCode(), "L1.hashCode() == L2.hashCode()")

    var l3: Any = l2
    assertEquals(l1.hashCode(), l3.hashCode(), "Any(Long).hashCode()")

    return "OK"
}