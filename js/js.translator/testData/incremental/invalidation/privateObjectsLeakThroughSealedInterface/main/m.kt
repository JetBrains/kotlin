fun box(stepId: Int): String {
    val x = test()
    if (x != stepId) {
        return "Fail $x != $stepId"
    }
    return "OK"
}
