fun box(stepId: Int, isWasm: Boolean): String {
    val x = test(stepId)
    when (stepId) {
        0 -> if (x != 5) return "Fail, x == $x"
        1 -> if (x != 32) return "Fail, x == $x"
        2 -> if (x != 0) return "Fail, x == $x"
        3 -> if (x != 0) return "Fail, x == $x"
        else -> return "Unkown"
    }
    return "OK"
}
