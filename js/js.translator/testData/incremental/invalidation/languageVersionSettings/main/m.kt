fun box(stepId: Int): String {
    when (stepId) {
        in 0..7 -> if (demo() != 42) return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
