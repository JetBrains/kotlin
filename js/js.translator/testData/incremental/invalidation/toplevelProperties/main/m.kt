fun box(stepId: Int): String {
    val r = testToplevelProperties()
    if (r != stepId) {
        return "Fail, got $r"
    }
    return "OK"
}
