fun box(stepId: Int, isWasm: Boolean): String {
    val a = ClassA()
    when (stepId) {
        0 -> {
            if (a.testPropertyWithGetter() != "0") return "Fail testPropertyWithGetter()"
            if (a.testPropertyWithSetter() != "test 0") return "Fail testPropertyWithSetter()"
        }
        1 -> {
            if (a.testPropertyWithGetter() != "1") return "Fail testPropertyWithGetter()"
            if (a.testPropertyWithSetter() != "test 0") return "Fail testPropertyWithSetter()"
        }
        2 -> {
            if (a.testPropertyWithGetter() != "1") return "Fail testPropertyWithGetter()"
            if (a.testPropertyWithSetter() != "test 2") return "Fail testPropertyWithSetter()"
        }
        else -> return "Unknown"
    }
    return "OK"
}
