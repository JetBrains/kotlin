fun box(stepId: Int, isWasm: Boolean): String {
    val x = qux()
    if (x != stepId) {
        return "Fail: $x != $stepId"
    }
    return "OK"
}
