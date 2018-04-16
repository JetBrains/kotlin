// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1113
package foo

fun test(f: () -> String): String {
    val funLit = { f() }
    return funLit()
}


fun box(): String {
    return test { "OK" }
}