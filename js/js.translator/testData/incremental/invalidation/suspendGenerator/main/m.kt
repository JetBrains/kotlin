fun box(stepId: Int): String {
    val got = test()
    if (got != stepId) {
        return  "Fail: $got != $stepId"
    }
    return "OK"
}
