// EXPECTED_REACHABLE_NODES: 487
package foo

fun box(): String {

    val a = 50;
    var b = 0;
    var c = 0;
    do {
        b = b + 1;
        c = c + 2;
    }
    while (b < a)
    if (c == 100) {
        return "OK"
    }
    return "fail: $c"
}

