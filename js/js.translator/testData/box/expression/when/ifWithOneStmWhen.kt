// EXPECTED_REACHABLE_NODES: 990
package foo


fun box(): String {
    var result = "fail1"
    var i = 1
    if (i==1)
        when (i) {
            1 ->    result = "OK"
            else -> result = "fail2"
        }
    return result
}
