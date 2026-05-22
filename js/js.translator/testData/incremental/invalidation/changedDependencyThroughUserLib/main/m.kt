fun box(stepId: Int, isWasm: Boolean): String {
    val expected = when (stepId) {
        0 -> 159
        1 -> 170
        else -> return "Unknown"
    }
    val actual = bar() + bar2()
    return if (actual == expected) "OK" else "Fail: $actual"
}

