// EXPECTED_REACHABLE_NODES: 1286
// NO_INLINE
package foo

object Obj

private inline fun <reified T> check(a: Any): String {
    return if (a is T) "OK" else "fail"
}

fun box(): String {
    var x: Any = Obj
    return check<Obj>(x)
}
