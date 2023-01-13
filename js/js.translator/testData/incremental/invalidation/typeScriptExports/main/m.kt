fun box(stepId: Int): String {
    val x = MyClass(stepId).qux()
    if (x != stepId) {
        return "Fail: $x != $stepId"
    }
    return "OK"
}
