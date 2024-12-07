fun box(stepId: Int, isWasm: Boolean): String {
    val x = testDefaltParam(stepId)
    if (x != stepId) {
        return "Fail, got $x, expected $stepId"
    }
    return "OK"
}
