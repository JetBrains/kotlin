// EXPECTED_REACHABLE_NODES: 1289
package foo

enum class E {
    OK
}

fun box(): String {
    return E.OK.name
}