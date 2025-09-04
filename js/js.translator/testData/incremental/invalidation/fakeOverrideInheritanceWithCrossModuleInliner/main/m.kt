fun box(stepId: Int, isWasm: Boolean): String {
    val a = ClassABC()
    when (stepId) {
        0, 4 -> {
            if (a.testA() != "a-0") return "Fail testA()"
            if (a.testB() != "b-0 a-0") return "Fail testB()"
            if (a.testC() != "c-0 b-0 a-0") return "Fail testC()"
        }
        1, 5 -> {
            if (a.testA() != "a-1") return "Fail testA()"
            if (a.testB() != "b-0 a-1") return "Fail testB()"
            if (a.testC() != "c-0 b-0 a-1") return "Fail testC()"
        }
        2 -> {
            if (a.testA() != "a-1") return "Fail testA()"
            if (a.testB() != "b-2") return "Fail testB()"
            if (a.testC() != "c-0 b-2") return "Fail testC()"
        }
        3 -> {
            if (a.testA() != "a-0") return "Fail testA()"
            if (a.testB() != "b-2") return "Fail testB()"
            if (a.testC() != "c-0 b-2") return "Fail testC()"
        }
        else -> return "Unknown"
    }
    return "OK"
}
