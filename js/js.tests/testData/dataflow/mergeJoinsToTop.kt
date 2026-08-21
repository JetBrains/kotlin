// Soundness: joining divergent concrete facts yields Top.

interface Base
class A : Base
class B : Base

fun box(): String {
    var x = 1
    if (js("true") as Boolean) {
        x = 2
    }
    val y: Base = if (js("true") as Boolean) A() else B()
    return if (x > 0 && y is Base) "OK" else "FAIL"
}
