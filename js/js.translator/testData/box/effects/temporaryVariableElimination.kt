// TARGET_BACKEND: JS_IR
// CHECK_OPTIMIZED_JS

object Global {
    var state = -1
}

inline fun reset(init: Int, f: () -> Unit) {
    if (init < 0) throw Exception("reset init must be >= 0")
    if (Global.state >= 0) throw Exception("called reset inside of another reset")
    Global.state = init
    f()
    Global.state = -1
}
fun check(value: Int, expected: Int): Int {
    if (value != expected) throw Exception("expected $expected got $value")
    return value
}
fun read(expected: Int): Int = check(Global.state, expected)
fun write(expected: Int): Int {
    val v = read(expected)
    Global.state = v + 1
    return Global.state
}

fun box(): String {
    reset(4) {
        val a = 4
        val b = 6
        val x = read(4)
        val xx = x * a
        val y = read(4)
        val c = write(4)
        val yy = y * a
        val d = 2
        val f = { a * yy + b * xx }
        val g = object {
            fun r() = c - d
        }
        if (!(f() > a && g.r() > 0)) return "not ok"
    }
    reset(0) {
        val a = read(0)
        val b = a + 1
        var i = 0
        while (i <= 2) {
            val c = b + 1
            write(i)
            check(c, 2)
            i += 1
        }
    }
    return "OK"
}
