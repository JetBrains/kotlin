// EXPECTED_REACHABLE_NODES: 487
package foo


fun box(): String {
    var result = "fail1"
    for (i in arrayOf(1))
        when (i) {
            1 -> result = "OK"
            else -> result = "fail2"
        }
    return result
}

