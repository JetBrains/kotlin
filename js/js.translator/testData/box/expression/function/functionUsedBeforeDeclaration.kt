// EXPECTED_REACHABLE_NODES: 1109
package foo


fun box(): String {
    return f()
}

fun f(): String {
    return "OK"
}


