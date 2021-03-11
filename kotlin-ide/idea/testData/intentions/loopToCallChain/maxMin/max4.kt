// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'max()'"
// IS_APPLICABLE_2: false
fun getMaxLineWidth(list: List<Float>): Float {
    var max = 0.0f
    <caret>for (item in list) {
        max = Math.max(item, max)
    }
    return max
}
