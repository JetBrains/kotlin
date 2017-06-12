// EXPECTED_REACHABLE_NODES: 491
package foo

class A(var i: Int) {
    override fun toString() = "a$i"
}

fun box(): String {
    var p = A(2);
    var n = A(1);
    if ("$p$n" != "a2a1") {
        return "fail1"
    }
    if ("${A(10)}" != "a10") {
        return "fail2"
    }
    return "OK"
}

