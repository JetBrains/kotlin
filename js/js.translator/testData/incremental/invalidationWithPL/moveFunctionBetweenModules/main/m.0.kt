fun box(stepId: Int, isWasm: Boolean): String {
    val expected = when (stepId) {
        0 -> 0
        1 -> 1
        else -> return "Unknown"
    }
    val actual = test()
    return if (actual == expected) "OK" else "Fail; got $actual"
}
