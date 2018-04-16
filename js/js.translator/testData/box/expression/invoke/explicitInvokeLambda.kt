// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1109
package foo

fun box(): String {
    var foo = { x: String -> x + "K" }
    return foo.invoke("O")
}