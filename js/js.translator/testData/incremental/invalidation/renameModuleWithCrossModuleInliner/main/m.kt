fun box(stepId: Int, isWasm: Boolean): String {
    val x = foo()
    if (x != stepId) {
        return "Fail: $x != $stepId"
    }
    return "OK"
}
