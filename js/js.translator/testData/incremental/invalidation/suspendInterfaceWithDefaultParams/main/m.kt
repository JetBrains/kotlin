internal suspend fun testDefaltParam(stepId: Int): Int {
    return callFun(ClassA2())
}

private suspend fun ignoreIt() = ClassA1()

private suspend fun callFun(a: InterfaceA): Int {
    return a.functionA(0, "", false)
}

suspend fun box(stepId: Int): String {
    if (testDefaltParam(stepId) != stepId) {
        return "Fail"
    }
    return "OK"
}
