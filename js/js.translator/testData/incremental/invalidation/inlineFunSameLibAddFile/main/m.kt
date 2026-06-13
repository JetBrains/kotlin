fun box(stepId: Int, isWasm: Boolean): String {
    val c = C()
    bar(c)
    val expected = when (stepId) {
        0 -> 117
        1 -> 45
        else -> return "Unknown"
    }
    return if (c.x == expected) "OK" else "Fail: ${c.x}"
}

