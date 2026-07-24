fun box(stepId: Int, isWasm: Boolean): String {
    val expected = when (stepId) {
        0 -> "A0"
        1 -> "A1"
        else -> return "Unknown"
    }
    val actual = marker<String>("s")
    return if (actual == expected) "OK" else "Fail: $actual"
}

