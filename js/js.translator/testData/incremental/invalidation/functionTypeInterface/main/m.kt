fun box(stepId: Int, isWasm: Boolean): String {
    val s = doTest()
    val expected = when (stepId) {
        0 -> "class SuspendFunction0"
        1 -> "class SuspendFunction1"
        2 -> "class SuspendFunction7"
        3 -> "class Function3"
        4 -> "class Function6"
        5 -> "class Function6_"
        6 -> "class SuspendFunction6_"
        7 -> "class Any_"
        8 -> "class SuspendFunction8_"
        9 -> "_class SuspendFunction8_"
        10 -> "_class Function9_"
        11 -> "__"
        12 -> if (!isWasm) "class Function9_" else "class getString\$lambda_"
        13 -> "true_"
        14 -> "false_"
        // TODO: I would expect, that it should be SuspendFunction5_,
        //  but it seems a feature of Kotlin/JS runtime.
        // In Kotlin/JVM it also gives different strings.
        15 -> if (!isWasm) "class Function6_" else "class getString\$slambda_"
        else -> return "Unknown"
    }

    return if (s != expected) "Fail step $stepId:\nExpected: $expected\nbut got $s" else "OK"
}
