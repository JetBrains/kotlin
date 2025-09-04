fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0, 1, 5, 6 -> if (MyClass.NestedClass().foo() != stepId) return "Fail"
        2, 3, 4 -> if (MyClass.NestedClass().foo() != 1) return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
