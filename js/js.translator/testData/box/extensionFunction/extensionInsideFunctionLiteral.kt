// EXPECTED_REACHABLE_NODES: 493
package foo

class M() {
    var m = 0

    fun eval() {
        var d = {
            var c = fun Int.(): Int = this + 3
            m += 3.c()
        }
        d();
    }
}

fun box(): String {
    var a = M()
    if (a.m != 0) return "fail1: ${a.m}";
    a.eval()
    if (a.m != 6) return "fail2: ${a.m}";

    return "OK";
}