// EXPECTED_REACHABLE_NODES: 490
package foo

var c = 0

class A() {
    var  p = 0;
    init {
        c++;
    }
}

fun box(): String {
    ++A().p
    if (c != 1) {
        return "fail1: $c"
    }
    --A().p
    if (c != 2) {
        return "fail2: $c"
    }
    return "OK"
}