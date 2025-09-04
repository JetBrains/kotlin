fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0 -> if (dex(4, 2) != 11) return "Fail"
        1 -> if (dex(4, 2) != 10) return "Fail"
        2 -> if (dex(4, 2) != 9) return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
