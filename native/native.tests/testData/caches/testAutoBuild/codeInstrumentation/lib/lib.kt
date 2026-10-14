var counter = 0

fun sum(values: IntArray): Int {
    val copy = IntArray(values.size) { values[it] }
    for (value in copy) counter += value
    return counter
}
