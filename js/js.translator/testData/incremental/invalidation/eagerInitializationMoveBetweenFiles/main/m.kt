fun box(stepId: Int, isWasm: Boolean): String {
    return when (stepId) {
        0 -> if (z1 && !z2) "OK" else "Fail step0 z1=$z1 z2=$z2"
        1 -> if (!z1 && z2) "OK" else "Fail step1 z1=$z1 z2=$z2"
        else -> "Unknown"
    }
}
