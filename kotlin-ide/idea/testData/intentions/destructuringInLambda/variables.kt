data class XY(val x: String, val y: String)

fun convert(xy: XY, foo: (XY) -> String) = foo(xy)

fun foo(xy: XY) = convert(xy) { <caret>it ->
    val x = it.x
    val y = it.y
    x + y
}