// WITH_RUNTIME

data class XY(val x: String, val y: String)

fun foo(list: List<XY>) = list.map { <caret>it -> it.x + it.y }