fun box(stepId: Int, isWasm: Boolean): String {
    val expected = when (stepId) {
        0 -> "A,B"
        1 -> "A,B,C"
        else -> return "Unknown"
    }
    val actual = Mode.values().joinToString(",") { it.name }
    return if (actual == expected) "OK" else "Fail: $actual"
}
