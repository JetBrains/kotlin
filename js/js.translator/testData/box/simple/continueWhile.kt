// EXPECTED_REACHABLE_NODES: 487
package foo

fun box(): String {
    var i = 0
    var b = true
    while (i < 100) {
        ++i;
        if (i >= 1) {
            continue;
        }
        b = false;
    }

    return if (b) "OK" else "fail"
}