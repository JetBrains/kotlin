// EXPECTED_REACHABLE_NODES: 1249
package foo

fun box(): String {
    val a = 2;
    val b = 3;
    var c = 4;
    return if (a < c) "OK" else "fail"
}

