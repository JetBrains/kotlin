// EXPECTED_REACHABLE_NODES: 1280
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