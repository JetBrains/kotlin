actual fun calc(expression: String): Double {
    return expression.toDoubleOrNull()?.let { it * 2 } ?: 0.0
}