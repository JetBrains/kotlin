// EXPECTED_REACHABLE_NODES: 888
package foo


var baz = 0
fun withSideEffect(v: Int): Int {
    baz = v
    return v
}

fun box(): String {
    val al = ArrayList<Int>(withSideEffect(2))
    if (al.size != 0) return "fail1: ${al.size}"
    if (baz != 2) return "fail2: $baz"

    return "OK"
}