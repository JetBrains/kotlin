
fun box(stepId: Int): String {
    when (stepId) {
        0, 2 -> if (Demo(15) == Demo(15)) return "Fail"
        1, 3 -> if (Demo(15) != Demo(15)) return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
