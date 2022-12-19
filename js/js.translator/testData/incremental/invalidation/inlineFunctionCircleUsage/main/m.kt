fun box(stepId: Int): String {
    val x = funA()
    if (x != stepId) {
        return "Fail: $x != $stepId"
    }
    return "OK"
}
