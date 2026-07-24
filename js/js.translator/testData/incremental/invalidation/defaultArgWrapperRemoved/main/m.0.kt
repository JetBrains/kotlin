fun box(stepId: Int, isWasm: Boolean): String {
    return if (foo() == 42) "OK" else "Fail step0"
}
