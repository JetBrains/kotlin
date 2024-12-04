fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0 -> if (qux() != 44) return "Fail"
        1 -> if (qux() != 66) return "Fail"
        2 -> if (qux() != 40) return "Fail"
        3 -> if (qux() != 18) return "Fail"
        4 -> if (qux() != 14) return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
