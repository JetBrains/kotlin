// EXPECTED_REACHABLE_NODES: 493
package foo

var a = MyInt()

class MyInt() {
    var b = 0

    operator fun inc(): MyInt {
        b = b + 1;
        return this;
    }
}


fun box(): String {
    val d = a++;

    if (a.b != 1) return "fail1: ${a.b}"
    if (d.b != 1) return "fail2: ${d.b}"

    return "OK"
}