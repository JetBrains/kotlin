fun box(stepId: Int, isWasm: Boolean): String {
    val obj = Module2Class()
    when (stepId) {
        0, 1, 2, 3, 4, 5, 6, 7, 8 -> {
            if (obj.testFunction1() != 1) return "Fail 1 class"
            if (obj.testFunction22() != 22) return "Fail 22 class"
            if (Module2Object.testFunction1() != 1) return "Fail 1 object"
            if (Module2Object.testFunction22() != 22) return "Fail 22 object"
        }
        else -> return "Unknown"
    }
    return "OK"
}
