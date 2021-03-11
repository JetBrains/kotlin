// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'map{}.max()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().map{}.max()'"
fun getMaxLineWidth(lineCount: Int): Float {
    var max_width = 0.0f
    <caret>for (i in 0..lineCount - 1) {
        val width = getLineWidth(i)
        max_width = if (width > max_width) width else max_width
    }
    return max_width
}

fun getLineWidth(i: Int): Float = TODO()
