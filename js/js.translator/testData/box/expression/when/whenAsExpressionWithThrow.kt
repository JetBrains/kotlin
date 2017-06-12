// EXPECTED_REACHABLE_NODES: 488
package foo

fun box(): String {

    try {
        (when (1) {
            3 -> {
                3
            }
            1 -> {
                throw Exception()
            }
            else -> {
                return "fail1"
            }
        })
    } catch (e: Exception) {
        return "OK"
    }

    return "fail2"
}