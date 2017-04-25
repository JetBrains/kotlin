// EXPECTED_REACHABLE_NODES: 490
package foo

var p = 0
val c = p++ // creates temporary value

fun box(): String {
    return if ((p == 1) && (c == 0)) "OK" else "fail"
}