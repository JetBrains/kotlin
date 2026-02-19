fun box(stepId: Int, isWasm: Boolean): String {
    val r = testToplevelProperties()
    if (r != stepId) {
        return "Fail, got $r"
    }
    return "OK"
}
