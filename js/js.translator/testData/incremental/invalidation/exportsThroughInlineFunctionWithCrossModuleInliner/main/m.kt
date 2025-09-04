fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0, 7 -> if (lib1Foo() != "empty") return "Fail"
        1, 4 -> if (lib1Foo() != "hello") return "Fail"
        2 -> if (lib1Foo() != "hello inline 0") return "Fail"
        3 -> if (lib1Foo() != "hello inline 3") return "Fail"
        5 -> if (lib1Foo() != "hello 5") return "Fail"
        6 -> if (lib1Foo() != "hello 6") return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
