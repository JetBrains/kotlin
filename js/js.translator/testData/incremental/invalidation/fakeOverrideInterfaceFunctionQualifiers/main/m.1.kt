fun box(stepId: Int): String {
    val obj = Module1Class()
    when (stepId) {
        0, 1, 2, 3, 4, 5, 6, 7, 8 -> {
            if (obj.testFunction1() != 1) return "Fail 1"
            if (obj.testFunction22() != 22) return "Fail 22"
        }
        else -> return "Unknown"
    }
    return "OK"
}
