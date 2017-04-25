// EXPECTED_REACHABLE_NODES: 550
package foo

enum class B(open val bar: Int) {
    a(0) {
        override val bar = 3
        init {
            y = 0
        }
    },
    b(4) {
    },
    c(5);
    val x = 1
    var y = 12;
}

interface X {
    val foo: Int
    fun bar(): Int {
        return foo;
    }
}

enum class Y(override val foo: Int) : X {
    m(3),
    n(6)
}

fun box(): String {
    if (B.a.x != 1) return "B.a.x != 1, it: ${B.a.x}"
    if (B.a.y != 0) return "B.a.y != 0, it: ${B.a.y}"
    if (B.a.bar != 3) return "B.a.bar != 3, it: ${B.a.bar}"

    if (B.b.y != 12) return "B.b.y != 12, it: ${B.b.y}"
    if (B.b.bar != 4) return "B.b.bar != 4, it: ${B.b.bar}"

    if (B.c.bar != 5) return "B.c.bar != 5, it: ${B.c.bar}"
    if (B.c.y != 12) return "B.c.y != 12, it: ${B.c.y}"

    if (Y.m.bar() != 3) return "Y.m.bar() != 3, it: ${Y.m.bar()}"
    if (Y.n.bar() != 6) return "Y.n.bar() != 6, it: ${Y.m.bar()}"

    return "OK"
}