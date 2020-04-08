// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'mapIndexed{}.max()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().mapIndexed{}.max()'"
fun getMaxLineWidth(list: List<Double>): Double {
    var max = 0.0
    <caret>for ((i, item) in list.withIndex()) {
        max = Math.max(max, item * i)
    }
    return max
}
