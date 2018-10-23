// EXPECTED_REACHABLE_NODES: 1280
package foo

fun box(): String {
    return "Fail${return "OK"}${return "Fail2"}"
}
