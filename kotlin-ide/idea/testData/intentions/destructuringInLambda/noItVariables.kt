// WITH_RUNTIME

data class XY(val x: String, val y: String)

fun convert(xy: XY, foo: (XY) -> Unit) = foo(xy)

fun foo(xy: XY) = convert(xy) <caret>{
    val x = it.x
    val y = it.y
    println(x + y)
}