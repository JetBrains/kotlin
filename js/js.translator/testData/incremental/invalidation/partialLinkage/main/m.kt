fun box(stepId: Int): String {
    val x = test()
    when (stepId) {
        in 0..2 -> if (stepId != x) return "Fail; got $x"
        else -> return "Unknown"
    }
    return "OK"
}
