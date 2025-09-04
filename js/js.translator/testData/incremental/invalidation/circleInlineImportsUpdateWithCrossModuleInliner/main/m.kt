fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0, 2 -> {
            if (f1() != "f1_2 -> f2_2 -> f3_2 -> f1_3 -> f2_3 -> f3_3 -> f1_4 -> stop") return "Fail"
        }
        1 -> {
            if (f1() != "f1_2 -> f2_2 -> f3_2 -> f1_3 -> f2_3 -> f3_3 -> f1_4 -> end") return "Fail"
        }
        else -> return "Unknown"
    }
    return "OK"
}
