fun box(stepId: Int): String {
    val x = testFunction()
    if (x != stepId) {
        return "Fail: $x != $stepId"
    }
    return "OK"
}
