fun box(stepId: Int): String {
    if (test() != stepId) return "Fail"
    return "OK"
}
