fun box(stepId: Int, isWasm: Boolean): String {
    val x = testFunction()
    if (x != stepId) {
        return "Fail: $x != $stepId"
    }
    return "OK"
}
