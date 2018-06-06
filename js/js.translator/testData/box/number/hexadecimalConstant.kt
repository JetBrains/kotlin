// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1108
package foo

fun box(): String {
    val i = 0x80000000 + 0x8000000
    return "OK"
}
