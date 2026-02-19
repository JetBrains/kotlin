fun box(stepId: Int, isWasm: Boolean): String {
    val x = test()
    if (x != stepId) return "Fail; got $x"
    return "OK"
}
