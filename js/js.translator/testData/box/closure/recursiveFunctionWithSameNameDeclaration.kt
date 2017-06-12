// EXPECTED_REACHABLE_NODES: 492
package foo

fun bar(i: Int = 0): Int {
    if (i == 7) {
        val bar = i
        return bar
    }
    else {
        return bar(i - 1)
    }
}

fun box(): String {
    val a = bar(10)
    if (a != 7) return "bar(10) = $a, but expected 7"

    fun boo(i: Int = 0): Int {
        if (i == 4) {
            val boo = i
            return boo
        } else {
            return boo(i - 1)
        }
    }
    val b = boo(17)
    if (b != 4) return "boo(17) = $b, but expected 4"

    fun f() = 1
    val v = 3
    fun baz(i: Int = 0): Int {
        if (i == v) {
            val baz = f() + v
            return baz
        } else {
            return baz(i - 1)
        }
    }

    val c = baz(10)
    if (c != 4) return "baz(10) = $c, but expected 4"

    return "OK"
}
