// EXPECTED_REACHABLE_NODES: 990
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