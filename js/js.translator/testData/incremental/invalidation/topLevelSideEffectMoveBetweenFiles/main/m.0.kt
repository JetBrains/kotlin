fun box(stepId: Int, isWasm: Boolean): String {
    sideEffectMarker
    val expected = when (stepId) {
        0 -> "A0"
        1 -> "A1"
        else -> return "Unknown"
    }
    return if (result == expected) "OK" else "Fail expected $expected actual $result"
}
