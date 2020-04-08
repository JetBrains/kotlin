data class XY(val x: String, val y: String)

fun convert(xy: XY, foo: (XY) -> String) = foo(xy)

fun foo(xy: XY) = convert(xy) <caret>{
    val (x, y) = it
    x + y
}