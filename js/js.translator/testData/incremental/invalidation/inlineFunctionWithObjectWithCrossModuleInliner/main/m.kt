fun box(stepId: Int, isWasm: Boolean): String {
    if (foo().doAnything() != stepId) {
        return "Fail"
    }
    return "OK"
}
