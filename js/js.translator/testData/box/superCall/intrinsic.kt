// EXPECTED_REACHABLE_NODES: 1297
class A(val x: Int) {
    override fun equals(other: Any?): Boolean = other is A && other.x == x

    override fun hashCode(): Int = x

    fun identityEquals(other: Any?) = super.equals(other)

    fun identityHashCode() = super.hashCode()
}

data class B(val y: Int) {
    fun identityEquals(other: Any?) = super.equals(other)

    fun identityHashCode() = super.hashCode()
}

fun box(): String {
    val a1 = A(23)
    val a2 = A(23)

    if (a1 != a2) return "fail1"
    if (a1.identityEquals(a2)) return "fail2"
    if (!a1.identityEquals(a1)) return "fail3"
    val ah: Any = a1.identityHashCode()
    if (ah !is Int) return "fail4"
    if (a1.hashCode() != 23) return "fail5"

    val b1 = B(23)
    val b2 = B(23)

    if (b1 != b2) return "fail6"
    if (b1.identityEquals(b2)) return "fail7"
    if (!b1.identityEquals(b1)) return "fail8"
    val bh: Any = a1.identityHashCode()
    if (bh !is Int) return "fail9"

    return "OK"
}