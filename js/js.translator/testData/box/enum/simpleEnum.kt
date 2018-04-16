// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1133
package foo

enum class E {
    OK
}

fun box(): String {
    return E.OK.name
}