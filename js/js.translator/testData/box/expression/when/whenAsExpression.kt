// EXPECTED_REACHABLE_NODES: 487
package foo


fun box(): String {

    val success = (when(1) {
        2 -> 3
        1 -> 1
        else -> 5
    } == 1)

    return if (success) "OK" else "fail"
}