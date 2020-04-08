// WITH_RUNTIME

data class XY(val x: String, val y: String)

fun foo(list: List<XY>) = list.fold("") { prev, <caret>xy -> prev + xy.x + xy.y }