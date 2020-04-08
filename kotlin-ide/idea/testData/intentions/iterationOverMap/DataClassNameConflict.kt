// WITH_RUNTIME

data class XY(val x: Int, val y: Int)

fun foo(list: List<XY>) {
    val y = list.size
    for (<caret>element in list) {
        val x = element.x + element.y
        println(x)
        println(y)
    }
}