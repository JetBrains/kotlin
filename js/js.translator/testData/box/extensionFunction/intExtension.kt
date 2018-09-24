// EXPECTED_REACHABLE_NODES: 1281
package foo

fun Int.quadruple(): Int {
    return this * 4;
}

fun box(): String {
    return if (3.quadruple() == 12) "OK" else "fail"
}
