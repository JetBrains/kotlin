fun box(stepId: Int, isWasm: Boolean): String {
    val x = test()
    val expected = when (stepId) {
        0, 1, 3, 4 -> stepId
        2 -> 1
        else -> return "Unknown"
    }
    if (expected != x) return "Fail; $expected != $x"

    return "OK"
}
