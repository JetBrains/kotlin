// WITH_RUNTIME

data class XY(val x: String, val y: Int)
fun test(xys: Array<XY>) {
    for (<caret>xy in xys) {
        val x = xy.x
        println(x)
        val xx = xy.x
        println(xx)
    }
}