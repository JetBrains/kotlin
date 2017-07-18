// EXPECTED_REACHABLE_NODES: 991
package foo

fun box(): String {
    var foo = { x: String -> x + "K" }
    return foo.invoke("O")
}