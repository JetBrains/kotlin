// WITH_RUNTIME

data class XY(val x: Int, val y: Int)
fun test(xys: Array<XY>) {
    xys.forEach { xy<caret> ->
        val x = xy.x
        println(x)
        val y = xy.y + x
        println(y)
    }
}