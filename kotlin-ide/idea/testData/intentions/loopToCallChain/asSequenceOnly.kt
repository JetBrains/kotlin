// WITH_RUNTIME
// IS_APPLICABLE: false
// INTENTION_TEXT_2: "Replace with 'asSequence().filter{}.forEach{}'"
fun getMaxLineWidth(lineCount: Int): Float {
    var max_width = 0.0f
    <caret>for (i in 0..lineCount - 1) {
        if (getLineWidth(i) > max_width) {
            max_width = getLineWidth(i)
        }
    }
    return max_width
}

fun  getLineWidth(i: Int): Float = TODO()
