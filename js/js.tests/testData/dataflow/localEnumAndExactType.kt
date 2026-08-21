// Precision: enum const + exact final type.

enum class Color { RED, GREEN }

interface Base
class Child : Base

fun box(): String {
    val e = Color.RED
    val x: Base = Child()
    if (e != Color.RED) return "FAIL"
    if (x !is Child) return "FAIL"
    return "OK"
}
