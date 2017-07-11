// EXPECTED_REACHABLE_NODES: 992
package foo

fun Int.same(): Int {
    return this
}

fun Int.quadruple(): Int {
    return same() * 4;
}

fun box(): String {
    return if ((3 + 4).quadruple() == 28) "OK" else "fail"
}
