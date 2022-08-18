fun box(stepId: Int): String {
    val obj = Module1Class()
    when (stepId) {
        0, 1, 2, 3, 4, 6, 7, 8 -> {
            if (obj.testFunction1() != 1) return "Fail 1"
            if (obj.testFunction22() != 22) return "Fail 22"
            if (obj.testField222 != 222) return "Fail 222"
        }
        5 -> {
            if (obj.testFunction1() != 1) return "Fail 1"
            if (obj.testFunction22() != 220) return "Fail 220"
            if (obj.testField222 != 2220) return "Fail 2220"
        }
        else -> return "Unknown"
    }
    return "OK"
}
