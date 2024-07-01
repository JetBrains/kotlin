fun box(stepId: Int, isWasm: Boolean): String {
    if (test() != stepId) return "Fail"
    return "OK"
}
