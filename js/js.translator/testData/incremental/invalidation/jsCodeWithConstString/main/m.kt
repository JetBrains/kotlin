fun box(stepId: Int): String {
    val x = doTest()
    when (stepId) {
        in 0..7 -> if (x != stepId) return "Fail, got $x"
        else -> return "Unknown"
    }
    return "OK"
}
