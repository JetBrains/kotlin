// EXPECTED_REACHABLE_NODES: 487
package foo

fun box(): String {
    val a = 10
    val b = 3
    when {
        a > b -> return "OK"
        b > a -> return "b"
        else -> return "else"
    }
}