fun box(stepId: Int): String {
    if (foo().doAnything() != stepId) {
        return "Fail"
    }
    return "OK"
}
