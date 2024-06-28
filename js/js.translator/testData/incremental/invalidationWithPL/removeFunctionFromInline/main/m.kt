fun box(stepId: Int): String {
    val x = test()
    if (x != stepId) return "Fail; got $x"
    return "OK"
}
