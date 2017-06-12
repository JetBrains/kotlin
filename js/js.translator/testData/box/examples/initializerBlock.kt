// EXPECTED_REACHABLE_NODES: 489
class C() {
    public var f: Int

    init {
        f = 610
    }
}

fun box(): String {
    val c = C()
    if (c.f != 610) return "fail"
    return "OK"
}
