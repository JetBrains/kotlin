// EXPECTED_REACHABLE_NODES: 1296
package foo

// CHECK_CONTAINS_NO_CALLS: myAdd

internal data class IntPair(public var fst: Int, public var snd: Int) {
    inline public fun getFst(): Int { return fst }
    inline public fun setFst(v: Int) { fst = v }

    inline public fun getSnd(): Int { return this.snd }
    inline public fun setSnd(v: Int) { this.snd = v }
}

// CHECK_BREAKS_COUNT: function=myAdd count=0
// CHECK_LABELS_COUNT: function=myAdd name=$l$block count=0
internal fun myAdd(p: IntPair, toFst: Int, toSnd: Int) {
    val fst = p.getFst()
    p.setFst(fst + toFst)

    val snd = p.getSnd()
    p.setSnd(snd + toSnd)
}

fun box(): String {
    val p = IntPair(0, 0)
    myAdd(p, 1, 2)
    assertEquals(IntPair(1, 2), p)

    return "OK"
}