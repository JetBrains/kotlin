// EXPECTED_REACHABLE_NODES: 1282
package foo

fun Int.same(): Int {
    return this
}

fun Int.quadruple(): Int {
    return same() * 4;
}

fun box(): String {
    return if (3.quadruple() == 12) "OK" else "fail"
}