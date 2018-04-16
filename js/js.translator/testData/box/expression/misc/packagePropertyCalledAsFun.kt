// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1111
package foo

fun lold() = true

val p = { { lold() }() }

fun box(): String {
    if (!p()) return "fail1"
    if (!foo.p()) return "fail2"
    return "OK"
}