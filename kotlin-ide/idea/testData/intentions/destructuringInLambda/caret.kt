// IS_APPLICABLE: false

data class XY(val x: String, val y: String)

fun convert(xy: XY, foo: (XY) -> String) = foo(xy)

fun foo(xy: XY) = convert<caret>(xy) { it.x + it.y }