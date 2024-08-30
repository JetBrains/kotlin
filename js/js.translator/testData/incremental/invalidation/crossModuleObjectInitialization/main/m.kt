fun box(stepId: Int, isWasm: Boolean): String {
    return if (X.result == stepId.toString()) "OK" else "FAIL Expected ${stepId.toString()} actual ${X.result}"
}
