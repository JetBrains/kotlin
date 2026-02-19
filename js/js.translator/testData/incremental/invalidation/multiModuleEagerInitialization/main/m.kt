fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0, 3 -> {
            if (z1) return "Fail z1"
            if (z2) return "Fail z2"
        }
        1 -> {
            if (!z1) return "Fail z1"
            if (z2) return "Fail z2"
        }
        2 -> {
            if (!z1) return "Fail z1"
            if (!z2) return "Fail z2"
        }
        else -> return "Unknown"
    }
    return "OK"
}
