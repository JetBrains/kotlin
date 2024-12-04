fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0 -> {
            if (funA() != "fun1") return "Fail funA"
            if (funB() != "fun2") return "Fail funB"
        }
        1 -> {
            if (funA() != "fun1") return "Fail funA"
            if (funB() != "fun2, fun1") return "Fail funB"
        }
        2 -> {
            if (funA() != "fun3") return "Fail funA"
            if (funB() != "fun2, fun1") return "Fail funB"
        }
        3 -> {
            if (funA() != "fun1") return "Fail funA"
            if (funB() != "fun2, fun3") return "Fail funB"
        }
        4 -> {
            if (funA() != "fun1") return "Fail funA"
            if (funB() != "fun3") return "Fail funB"
        }
        5 -> {
            if (funA() != "fun4") return "Fail funA"
            if (funB() != "fun1") return "Fail funB"
        }
        6 -> {
            if (funA() != "fun4, fun3") return "Fail funA"
            if (funB() != "fun1, fun2, fun4") return "Fail funB"
        }
        7 -> {
            if (funA() != "fun4") return "Fail funA"
            if (funB() != "fun1") return "Fail funB"
        }
        8 -> {
            if (funA() != "fun1, fun3") return "Fail funA"
            if (funB() != "fun4") return "Fail funB"
        }
        9 -> {
            if (funA() != "fun4") return "Fail funA"
            if (funB() != "fun1, fun3, fun2") return "Fail funB"
        }
        else -> return "Unknown"
    }
    return "OK"
}
