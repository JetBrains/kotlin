import bridge.bar

fun box(stepId: Int, isWasm: Boolean): String {
    val expected = when (stepId) {
        0 -> "A0"
        1 -> "B1"
        else -> return "Unknown"
    }
    val actual = bar()
    return if (actual == expected) "OK" else "Fail: $actual"
}
