fun box(stepId: Int): String {
    val x = qux()
    if (x != stepId) {
        return "Fail: $x != $stepId"
    }
    return "OK"
}
