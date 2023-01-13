fun box(stepId: Int): String {
    val x = foo().toInt()
    if (x != stepId) {
        return "Fail $x != $stepId"
    }
    return "OK"
}
