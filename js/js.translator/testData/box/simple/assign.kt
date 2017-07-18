// EXPECTED_REACHABLE_NODES: 991
package foo

fun f(): Int {
    var x: Int = 1
    x = x + 1
    return x
}

fun box() = if (f() == 2) "OK" else "fail"