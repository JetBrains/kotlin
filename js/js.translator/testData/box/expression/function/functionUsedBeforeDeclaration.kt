// EXPECTED_REACHABLE_NODES: 991
package foo


fun box(): String {
    return f()
}

fun f(): String {
    return "OK"
}


