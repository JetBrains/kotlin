data class XY(val x: Int, val y: Int)

fun create() = XY(1, 2)

annotation class Ann

fun use(): Int {
    @Ann val <caret>xy = create()
    return xy.x + xy.y
}