package foo

enum class B(open val bar: Int) {
    val x = 1
    var y = 12;
    a : B(0) {
        override val bar = 3
        {
            y = 0
        }
    }
    b : B(4){}
    c : B(5)
}

trait X {
    val foo: Int
    fun bar(): Int {
        return foo;
    }
}

enum class Y(override val foo: Int) : X {
    m:Y(3)
    n:Y(6)
}

fun box(): String {
    if (B.a.x != 1) return "B.a.x != 1"
    if (B.a.y != 0) return "B.a.y != 0"
    if (B.a.bar != 3) return "B.a.bar != 3"

    if (B.b.bar != 4) return "B.b.bar != 4"
    if (B.b.y != 12) return "B.b.y != 12"

    if (B.c.bar != 5) return "B.c.bar != 5"
    if (B.c.y != 12) return "B.c.y != 12"

    if (Y.m.bar() != 3) return "Y.m.bar() != 3"
    if (Y.n.bar() != 6) return "Y.n.bar() != 6"

    return "OK"
}