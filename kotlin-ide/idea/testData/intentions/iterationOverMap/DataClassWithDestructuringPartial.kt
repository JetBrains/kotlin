// WITH_RUNTIME

data class XY(val x: Int, val y: Int)

fun foo(list: List<XY>): Int {
    var result = 0
    for (element<caret> in list) {
        val (x) = element
        result += x
        result += element.y
    }
    return result
}