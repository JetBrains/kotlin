fun box(stepId: Int): String {
    val x = foo()
    if (x != stepId) {
        return "Fail: $x != $stepId"
    }
    return "OK"
}
