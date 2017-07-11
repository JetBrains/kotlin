// EXPECTED_REACHABLE_NODES: 990
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