// EXPECTED_REACHABLE_NODES: 491
package foo

class MyInt(i: Int) {
    var b = i
    operator fun inc(): MyInt {
        b = b++;
        return this;
    }
}

fun box(): String {
    var t = MyInt(0)
    t++;
    return if (t.b == 0) "OK" else "fail: ${t.b}"
}