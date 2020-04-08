// WITH_RUNTIME
class C(val x: Int, val y: Int, var z: Int)

class CBuilder {
    var x: Int = 0
    var y: Int = 0
    fun build() = C(x, y, 0)
}

fun test(<caret>a: C) =
    CBuilder().apply {
        x = a.x
        y = a.y
    }.build().apply {
        val b = this
        z = b.x + b.y
    }