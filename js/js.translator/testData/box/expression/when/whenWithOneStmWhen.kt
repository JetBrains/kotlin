// EXPECTED_REACHABLE_NODES: 487
package foo


fun box(): String {
    var result = "fail1"
    val i = 1
    when (i) {
        1 ->
            when (i) {
                1 ->    result = "OK"
                else -> result = "fail2"
            }

        else ->
            when (i) {
                1 ->    result = "OK"
                else -> result = "fail3"
            }
    }

    return result
}
