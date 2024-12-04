fun box(stepId: Int, isWasm: Boolean): String {
    val obj = Module2Class()
    when (stepId) {
        0, 1, 2, 3, 4, 6, 7, 8 -> {
            if (obj.testFunction1() != 1) return "Fail 1 class"
            if (obj.testFunction22() != 22) return "Fail 22 class"
            if (obj.testField222 != 222) return "Fail 222 class"

            if (Module2Object.testFunction1() != 1) return "Fail 1 object"
            if (Module2Object.testFunction22() != 22) return "Fail 22 object"
            if (Module2Object.testField222 != 222) return "Fail 222 object"
        }
        5 -> {
            if (obj.testFunction1() != 1) return "Fail 1 class"
            if (obj.testFunction22() != 220) return "Fail 220 class"
            if (obj.testField222 != 2220) return "Fail 2220 class"

            if (Module2Object.testFunction1() != 1) return "Fail 1 object"
            if (Module2Object.testFunction22() != 220) return "Fail 220 object"
            if (Module2Object.testField222 != 2220) return "Fail 2220 object"
        }
        else -> return "Unknown"
    }
    return "OK"
}
