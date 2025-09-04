fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0 -> {
            if (funA() != "fun1") return "Fail funA"
            if (funB() != "fun2") return "Fail funB"
        }
        1 -> {
            if (funA() != "fun2") return "Fail funA"
            if (funB() != "fun3") return "Fail funB"
        }
        2 -> {
            if (funA() != "fun2-2") return "Fail funA"
            if (funB() != "fun3-2") return "Fail funB"
        }
        3, 4 -> {
            if (funA() != "fun2-2") return "Fail funA"
            if (funB() != "fun1-3") return "Fail funB"
        }
        5 -> {
            if (funA() != "fun1-3") return "Fail funA"
            if (funB() != "fun1-3") return "Fail funB"
        }
        6 -> {
            if (funA() != "fun1-6") return "Fail funA"
            if (funB() != "fun1-6") return "Fail funB"
        }
        7 -> {
            if (funA() != "fun2-2") return "Fail funA"
            if (funB() != "fun3-4") return "Fail funB"
        }
        8 -> {
            if (funA() != "fun2-8") return "Fail funA"
            if (funB() != "fun3-8") return "Fail funB"
        }
        else -> return "Unknown"
    }
    return "OK"
}
