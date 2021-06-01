inline class Z(val data: Int)

val xs = Array(2) { Z(42) }

fun box(): String {
    xs[0] = Z(12)
    val t = xs[0]
    if (t.data != 12) throw AssertionError("$t")

    return "OK"
}
