// EXPECTED_REACHABLE_NODES: 488
package foo


fun box(): String {
    return f()
}

fun f(): String {
    return "OK"
}


