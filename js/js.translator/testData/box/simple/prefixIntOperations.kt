// EXPECTED_REACHABLE_NODES: 1280
package foo

fun box(): String {
    var a = 3;
    val b = ++a;
    --a;
    --a;
    return if ((--a == 1) && (b == 4)) "OK" else "fail"
}

