// EXPECTED_REACHABLE_NODES: 1280
package foo

fun box(): String {
    var a = 3;
    val b = a++;
    a--;
    a--;
    return if ((a++ == 2) && (b == 3)) "OK" else "fail"
}

