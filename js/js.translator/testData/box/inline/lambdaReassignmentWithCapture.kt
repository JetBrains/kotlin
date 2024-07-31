// EXPECTED_REACHABLE_NODES: 1295
package foo

data class IntPair(public var fst: Int, public var snd: Int)

inline fun run(func: () -> Int): Int {
    return func()
}

// CHECK_BREAKS_COUNT: function=bar count=0
// CHECK_LABELS_COUNT: function=bar name=$l$block count=0
fun bar(p: IntPair): Int {
    var f = { -> p.fst++ }
    var get0 = f

    f = { -> ++p.snd }
    var get1 = f

    var get2 = get1
    f = { -> ++p.fst }
    get2 = f

    return run(get0) + run(get1) + run(get2)
}

fun box(): String {
    val p = IntPair(0, 0)
    assertEquals(3, bar(p))
    assertEquals(IntPair(2, 1), p)

    return "OK"
}