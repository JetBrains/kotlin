fun box(stepId: Int, isWasm: Boolean): String = when (stepId) {
        0 -> if (z1) "OK" else "Fail step0 z1=$z1"
        1 -> if (!z1) "OK" else "Fail step1 z1=$z1"
    else -> "Unknown"
}
