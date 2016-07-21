fun when_expression_1(x: Int): Int {
    val z = when (x) {
        21234 -> 20
        55555 -> 50
        else -> 100
    }
    return z
}