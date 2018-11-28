// EXPECTED_REACHABLE_NODES: 1282
package foo

var i = 0

inline fun f() = i * 2

fun box(): String {
    return if ((++i + f()) == 3) "OK" else "fail"
}