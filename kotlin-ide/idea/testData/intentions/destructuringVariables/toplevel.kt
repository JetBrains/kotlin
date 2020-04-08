// IS_APPLICABLE: false

data class XY(val x: Int, val y: Int)

fun create() = XY(1, 2)

val xy = <caret>create()