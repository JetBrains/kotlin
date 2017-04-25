// EXPECTED_REACHABLE_NODES: 487
package foo

fun box(): String {
    var i = 0
    while (i < 100) {
        if (i == 3) {
            break;
        }
        ++i;
    }

    return if (i == 3) "OK" else "fail: $i"
}