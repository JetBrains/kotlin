// EXPECTED_REACHABLE_NODES: 490
fun f(x: Int) = x * 2

fun test(f: (Long) -> Long) = Pair(f(23 as Int), f(42L))

fun box(): String {
    val result = test { it * 3 }
    if (result != Pair(46, 126L)) return "fail: $result"
    return "OK"
}