// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1096

package foo

fun box(): String {
    val map = mapOf(1 to "")
    val i = 1
    return when (i) {
        in map -> "OK"
        else -> "fail"
    }
}