fun box(stepId: Int): String {
    val x = test()
    if (stepId != x) return "Fail; got $x"
    return "OK"
}
