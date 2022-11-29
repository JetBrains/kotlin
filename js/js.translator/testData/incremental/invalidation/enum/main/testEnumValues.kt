fun testEnumValues(stepId: Int): Boolean {
    val values = enumValues<TestEnum>().map { it.ordinal to it.name }
    when (stepId) {
        0 -> if (values.isEmpty()) return true
        1, 2, 3 -> if (values == listOf(0 to "A", 1 to "B")) return true
        else -> return false
    }
    return false
}
