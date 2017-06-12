// EXPECTED_REACHABLE_NODES: 491
package foo

class MyInt() {
    var b = 0

    operator fun dec(): MyInt {
        val res = MyInt()
        res.b++;
        return res;
    }
}


fun box(): String {
    var c = MyInt()
    --c;
    return if (c.b == 1) "OK" else "fail: ${c.b}"
}