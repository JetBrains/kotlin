fun box(stepId: Int, isWasm: Boolean): String {
    val x = test()
    val expected = when (stepId) {
        0, 1, 2 -> stepId
        3 -> 2
        else -> return "Unknown"
    }
    if (expected != x) return "Fail on step $stepId; $expected != $x"
    return "OK"
}
