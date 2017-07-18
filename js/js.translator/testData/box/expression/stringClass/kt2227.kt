// EXPECTED_REACHABLE_NODES: 990
package foo

fun box(): String {
    if ("${3}" != "3") return "fail1"
    return if ("${3}" == "3") "OK" else "fail2"
}