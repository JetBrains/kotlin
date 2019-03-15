// EXPECTED_REACHABLE_NODES: 1281
package foo


fun box(): String {
    return f()
}

fun f(): String {
    return "OK"
}


