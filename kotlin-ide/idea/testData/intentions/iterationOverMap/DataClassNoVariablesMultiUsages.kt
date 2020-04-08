// WITH_RUNTIME

data class XY(val x: String, val y: String)
fun test(xys: Array<XY>) {
    for (<caret>xy in xys) {
        println(xy.x + xy.y + xy?.x + xy?.y)
    }
}