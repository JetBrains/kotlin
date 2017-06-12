// EXPECTED_REACHABLE_NODES: 491
package foo

class MyInt() {
    var b = 0

    operator fun dec(): MyInt {
        b = b + 1;
        return this;
    }
}


fun box(): String {
    var c = MyInt()
    val d = --c;
    return if (c.b == 1) "OK" else "fail: ${c.b}"
}