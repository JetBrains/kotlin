// EXPECTED_REACHABLE_NODES: 487
package foo

fun box(): String {
    var i = 0
    var b = true
    do {
        ++i;
        if (i >= 1) {
            continue;
        }
        b = false;
    }
    while (i < 100)

    return if (b) "OK" else "fail"
}