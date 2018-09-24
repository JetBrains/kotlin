// EXPECTED_REACHABLE_NODES: 1281
package foo

fun sum(param1: Int, param2: Int): Int {
    return param1 + param2;
}

fun box(): String {
    return if (sum(1, 5) == 6) "OK" else "fail"
}