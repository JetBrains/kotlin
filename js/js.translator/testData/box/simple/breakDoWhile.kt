// EXPECTED_REACHABLE_NODES: 1280
package foo

fun box(): String {
    var i = 0
    do {
        if (i == 3) {
            break;
        }
        ++i;
    }
    while (i < 100)

    return if (i == 3) "OK" else "fail: $i"
}