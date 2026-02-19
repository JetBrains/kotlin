fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0, 2, 4, 6, 7 -> if (qux() != stepId) return "Fail"
        1, 3, 5, 8 -> {
            try {
                val r = qux()
                return "Fail, got $r"
            } catch (e: Exception) {
            }
        }

        else -> return "Unknown"
    }
    return "OK"
}
