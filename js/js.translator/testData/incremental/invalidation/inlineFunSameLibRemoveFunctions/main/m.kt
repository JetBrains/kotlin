fun box(stepId: Int, isWasm: Boolean): String {
    val expected = when (stepId) {
        0 -> 42
        1 -> 43
        else -> return "Unknown"
    }
    val actual = compute()
    return if (actual == expected) "OK" else "Fail: $actual"
}

