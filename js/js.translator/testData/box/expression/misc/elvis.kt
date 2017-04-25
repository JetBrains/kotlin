// EXPECTED_REACHABLE_NODES: 489
package foo

fun box(): String {
    if (f(null) != false) {
        return "fail1"
    }
    if (f(2) != true) {
        return "fail2"
    }
    if (f(1) != false) {
        return "fail3"
    }
    return "OK"
}

fun Int.isEven() = (this % 2) == 0

fun f(a: Int?): Boolean {
    return a?.isEven() ?: false

}