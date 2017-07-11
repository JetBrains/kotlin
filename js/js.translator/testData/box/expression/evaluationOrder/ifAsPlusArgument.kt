// EXPECTED_REACHABLE_NODES: 990
package foo

fun box(): String {
    var  i = 0
    var t = ++i + if (i == 0) 0 else 2
    if (t != 3) {
        return "fail: $t"
    }
    return "OK"
}
