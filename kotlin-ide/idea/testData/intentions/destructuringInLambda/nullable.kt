// IS_APPLICABLE: false
// WITH_RUNTIME

data class XY(val x: String, val y: String)
fun test(xys: Array<XY?>) {
    xys.forEach { xy<caret> ->
        val x = xy?.x
        val y = xy?.y
        println(x + y)
    }
}