// Changed when traits were introduced. May not make sense any more

open class Base() {
    public var v: Int = 0
}

open class Left() : Base() {
}
trait Right : Base {
}

class D() : Left(), Right

fun vl(l: Left): Int = l.v
fun vr(r: Right): Int = r.v

fun box(): String {
    val d = D()
    d.v = 42

    if (d.v != 42) return "Fail #1"
    if (vl(d) != 42) return "Fail #2"
    if (vr(d) != 42) return "Fail #3"

    return "OK"
}