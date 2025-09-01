fun box(stepId: Int, isWasm: Boolean): String {
    val x = test()
    val expected = when (stepId) {
        0 -> stepId
        1, 2, 3, 4 -> 0
        else -> return "Unknown"
    }
    if (expected != x) return "Fail on step $stepId; $expected != $x"

    return "OK"
}
