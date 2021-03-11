// WITH_RUNTIME

data class XY(val x: Int, val y: Int)

fun foo(list: List<XY>) {
    for (element<caret> in list) {
        val z = element.y
        val (x) = element
    }
}