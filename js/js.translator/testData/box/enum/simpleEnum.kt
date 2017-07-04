// EXPECTED_REACHABLE_NODES: 1383
package foo

enum class E {
    OK
}

fun box(): String {
    return E.OK.name
}