// EXPECTED_REACHABLE_NODES: 1110
package foo

fun box(): String {
    return "Fail${return "OK"}${return "Fail2"}"
}
