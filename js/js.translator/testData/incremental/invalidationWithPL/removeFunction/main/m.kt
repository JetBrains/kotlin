fun box(stepId: Int, isWasm: Boolean): String {
    val x = test()
    if (stepId != x) return "Fail; got $x"
    return "OK"
}
