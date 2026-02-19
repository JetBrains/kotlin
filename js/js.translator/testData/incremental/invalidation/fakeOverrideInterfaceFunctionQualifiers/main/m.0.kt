fun box(stepId: Int, isWasm: Boolean): String {
    val obj = Module2Class()
    when (stepId) {
        0, 1, 2, 3, 4, 5, 6, 7, 8 -> {
            if (obj.testFunction1() != 1) return "Fail 1 class"
            if (Module2Object.testFunction1() != 1) return "Fail 1 object"
        }
        else -> return "Unknown"
    }
    return "OK"
}
