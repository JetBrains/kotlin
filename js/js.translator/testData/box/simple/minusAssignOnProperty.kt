// EXPECTED_REACHABLE_NODES: 991
package foo

var a = 3

fun box(): String {
    a -= 10

    return if (a == -7) "OK" else "fail"

}