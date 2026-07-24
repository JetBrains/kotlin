fun box(stepId: Int, isWasm: Boolean): String {
    if (!isWasm) return "OK"

    if (Child.someGet != 0) return "Expected 0 found ${Child.someGet}"

    when (stepId) {
        0 -> if (staticInitParent) return "Expected uninitialized super class"
        1 -> if (!staticInitParent) return "Expected initialized super class"
        else -> return  "Unknown"
    }

    return "OK"
}
