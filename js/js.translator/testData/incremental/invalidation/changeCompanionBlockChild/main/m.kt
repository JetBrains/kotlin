fun box(stepId: Int, isWasm: Boolean): String {
    if (Child.someGet != stepId) return "Expected $stepId found ${Child.someGet}"
    if (!staticInitParent) return "Expected initialized super class"
    return "OK"
}
