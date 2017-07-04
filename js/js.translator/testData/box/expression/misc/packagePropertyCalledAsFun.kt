// EXPECTED_REACHABLE_NODES: 1377
package foo

fun lold() = true

val p = { { lold() }() }

fun box(): String {
    if (!p()) return "fail1"
    if (!foo.p()) return "fail2"
    return "OK"
}