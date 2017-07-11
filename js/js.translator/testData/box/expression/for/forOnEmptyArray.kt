// EXPECTED_REACHABLE_NODES: 994
package foo

val a1 = arrayOfNulls<Int>(0)

fun box(): String {
    for (a in a1) {
        return "fail"
    }
    return "OK"
}