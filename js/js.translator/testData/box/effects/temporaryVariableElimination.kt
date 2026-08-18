// TARGET_BACKEND: JS_IR
// CHECK_OPTIMIZED_JS

object Global {
    var state = 4
}

fun write(expected: Int): Int {
    val v = read(expected)
    Global.state = v + 2
    return Global.state
}
fun read(expected: Int): Int {
    val v = Global.state
    if (v != expected) throw Exception("expected $expected got $v")
    return v
}

fun box(): String {
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
    if (f() > a && g.r() > 0) return "OK"
    return "not ok"
}
