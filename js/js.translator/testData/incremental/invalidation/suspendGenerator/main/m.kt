fun box(stepId: Int, isWasm: Boolean): String {
    val got = test()
    if (got != stepId) {
        return  "Fail: $got != $stepId"
    }
    return "OK"
}
