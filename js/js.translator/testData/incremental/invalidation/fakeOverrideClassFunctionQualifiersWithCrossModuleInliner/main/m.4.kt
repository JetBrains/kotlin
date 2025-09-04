fun box(stepId: Int, isWasm: Boolean): String {
    val obj = Module1Class()
    when (stepId) {
        0, 1, 2, 3, 4, 10, 11 -> {
            if (obj.testFunction1() != 1) return "Fail 1"
            if (obj.testFunction22() != 22) return "Fail 22"
            if (obj.testField222 != 222) return "Fail 222"
        }
        5, 6, 7 -> {
            if (obj.testFunction1() != 1) return "Fail 1"
            if (obj.testFunction22() != 220) return "Fail 220"
            if (obj.testField222 != 2220) return "Fail 2220"
        }
        8 -> {
            if (obj.testFunction1() != 1) return "Fail 1"
            if (obj.testFunction22() != 2201) return "Fail 2201"
            if (obj.testField222 != 2220) return "Fail 2220"
        }
        9 -> {
            if (obj.testFunction1() != 1) return "Fail 1"
            if (obj.testFunction22() != 2201) return "Fail 2201"
            if (obj.testField222 != 22201) return "Fail 22201"
        }
        else -> return "Unknown"
    }
    return "OK"
}
