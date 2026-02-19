fun box(stepId: Int, isWasm: Boolean): String {
    val x = foo().toInt()
    if (x != stepId) {
        return "Fail $x != $stepId"
    }
    return "OK"
}
