fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0, 2 -> {
            if (f1() != "f1_1 f2_1 f3_1") return "Fail f1"
            if (f2() != "empty") return "Fail f2"
        }
        1, 3 -> {
            if (f1() != "f1_1 f2_1 f3_1") return "Fail f1"
            if (f2() != "something") return "Fail f2"
        }
        else -> return "Unknown"
    }
    return "OK"
}
