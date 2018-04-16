// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1111
package foo

var p = 0
val c = p++ // creates temporary value

fun box(): String {
    return if ((p == 1) && (c == 0)) "OK" else "fail"
}