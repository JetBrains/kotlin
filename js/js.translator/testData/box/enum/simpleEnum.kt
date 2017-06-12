// EXPECTED_REACHABLE_NODES: 516
package foo

enum class E {
    OK
}

fun box(): String {
    return E.OK.name
}