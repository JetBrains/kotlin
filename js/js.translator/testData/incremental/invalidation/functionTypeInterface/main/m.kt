fun box(stepId: Int, isWasm: Boolean): String {
    val s = doTest()
    when (stepId) {
        0 -> if (s == "class SuspendFunction0") return "OK"
        1 -> if (s == "class SuspendFunction1") return "OK"
        2 -> if (s == "class SuspendFunction7") return "OK"
        3 -> if (s == "class Function3") return "OK"
        4 -> if (s == "class Function6") return "OK"
        5 -> if (s == "class Function6_") return "OK"
        6 -> if (s == "class SuspendFunction6_") return "OK"
        7 -> if (s == "class Any_") return "OK"
        8 -> if (s == "class SuspendFunction8_") return "OK"
        9 -> if (s == "_class SuspendFunction8_") return "OK"
        10 -> if (s == "_class Function9_") return "OK"
        11 -> if (s == "__") return "OK"
        12 -> if (s == "class Function9_") return "OK"
        13 -> if (s == "true_") return "OK"
        14 -> if (s == "false_") return "OK"
        // TODO: I would expect, that it should be SuspendFunction5_,
        //  but it seems a feature of Kotlin/JS runtime.
        // In Kotlin/JVM it also gives different strings.
        15 -> if (s == "class Function6_") return "OK"
        else -> return "Unknown"
    }
    return "Fail; got $s"
}
