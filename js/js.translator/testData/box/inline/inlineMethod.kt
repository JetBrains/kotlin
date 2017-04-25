// EXPECTED_REACHABLE_NODES: 506
package foo

// CHECK_CONTAINS_NO_CALLS: add

internal data class IntPair(public var fst: Int, public var snd: Int) {
    inline public fun getFst(): Int { return fst }
    inline public fun setFst(v: Int) { fst = v }

    inline public fun getSnd(): Int { return this.snd }
    inline public fun setSnd(v: Int) { this.snd = v }
}

internal fun add(p: IntPair, toFst: Int, toSnd: Int) {
    val fst = p.getFst()
    p.setFst(fst + toFst)

    val snd = p.getSnd()
    p.setSnd(snd + toSnd)
}

fun box(): String {
    val p = IntPair(0, 0)
    add(p, 1, 2)
    assertEquals(IntPair(1, 2), p)

    return "OK"
}