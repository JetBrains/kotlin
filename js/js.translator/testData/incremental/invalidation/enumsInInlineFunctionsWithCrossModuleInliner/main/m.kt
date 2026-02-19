fun box(stepId: Int, isWasm: Boolean): String {
    val enums = testEnums()
    when (stepId) {
        0 -> if (enums.toString() != "[A, B, A, B]") return "Fail, got $enums"
        1 -> if (enums.toString() != "[A, B, A, B, A, B, C]") return "Fail, got $enums"
        2 -> if (enums.toString() != "[A, B, A, B]") return "Fail, got $enums"
        3 -> if (enums.toString() != "[A, B, A, B, A]") return "Fail, got $enums"
        4 -> if (enums.toString() != "[A, B]") return "Fail, got $enums"
        else -> return "Unknown"
    }

    for (i1 in 0..enums.size - 1) {
        for (i2 in 0..enums.size - 1) {
            if (i1 == i2) {
                continue
            }
            if (enums[i1] == enums[i2]) {
                return "Fail eq2"
            }
            if (enums[i1] === enums[i2]) {
                return "Fail eq3"
            }
        }
    }

    return "OK"
}
