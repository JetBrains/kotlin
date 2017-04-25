// EXPECTED_REACHABLE_NODES: 487
package foo


fun box(): String {
    var result = "fail1"
    var i = 1
    loop@ while(i==1)
        when (i) {
            1 -> { result = "OK"; break@loop }
            else -> result = "fail"
        }
    return result
}

