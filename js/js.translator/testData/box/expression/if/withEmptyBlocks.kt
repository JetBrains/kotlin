// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1280
package foo

fun box(): String {
    var r = ""
    if (r != "") else r += "O"
    if (r == "O") r += "K" else;
    return r
}
