// EXPECTED_REACHABLE_NODES: 487
package foo


fun box(): String {
    var result = "fail1"
    var i = 1
    do
        when (i) {
            1 ->    result = "OK"
            else -> result = "fail2"
        }
    while (i==0)
    return result
}

