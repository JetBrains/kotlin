// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'map{}.min()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().map{}.min()'"
fun getMinLineWidth(lineCount: Int): Double {
    var min_width = Double.MAX_VALUE
    <caret>for (i in 0..lineCount - 1) {
        val width = getLineWidth(i)
        min_width = if (min_width >= width) width else min_width
    }
    return min_width
}

fun getLineWidth(i: Int): Double = TODO()
