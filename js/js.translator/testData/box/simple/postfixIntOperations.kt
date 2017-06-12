// EXPECTED_REACHABLE_NODES: 487
package foo

fun box(): String {
    var a = 3;
    val b = a++;
    a--;
    a--;
    return if ((a++ == 2) && (b == 3)) "OK" else "fail"
}

