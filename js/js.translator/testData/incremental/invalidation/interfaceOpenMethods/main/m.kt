fun box(stepId: Int): String {
    val expected = when (stepId) {
        0 -> 10
        1 -> 13
        2 -> 16
        else -> return "Unknown"
    }

    val x = test()
    if (expected != x) {
        return "Fail $expected != $x"
    }

    return "OK"
}
