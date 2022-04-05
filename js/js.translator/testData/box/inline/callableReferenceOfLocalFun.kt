// SKIP_SOURCEMAP_REMAPPING

class TestData(var status: String) {
    fun updateStatus(newStatus: String) {
        status = newStatus
    }
}

inline fun <T> inlineGenericTestFunction(f: () -> T) = f()

inline fun inlineFunction(f: () -> Unit) = f()

fun testCase1(obj: TestData) = inlineFunction {
    class InlineClass

    val updateStatus = inlineGenericTestFunction {
        fun localFunction() { obj.status = "OK" }
        ::localFunction
    }
    updateStatus()
}

fun testCase2(obj: TestData) = inlineFunction {
    class InlineClass

    val updateStatus = inlineGenericTestFunction {
        fun localFunction(msg: String) { obj.status = msg }
        ::localFunction
    }
    updateStatus("OK")
}

fun testCase3(obj: TestData) = inlineFunction {
    class InlineClass

    val updateStatus = inlineGenericTestFunction {
        fun localFunction(flag: Boolean, msg: String) { obj.status = if (flag) msg else "ERROR" }
        ::localFunction
    }
    updateStatus(true, "OK")
}

fun testCase4(obj: TestData) = inlineFunction {
    class InlineClass

    val updateStatus: String.() -> Unit = inlineGenericTestFunction {
        fun String.localFunction() { obj.status = this }
        String::localFunction
    }
    updateStatus("ERROR")
    "OK".updateStatus()
}

fun testCase5(obj: TestData) = inlineFunction {
    class InlineClass

    val updateStatus: () -> Unit = inlineGenericTestFunction {
        fun String.localFunction() { obj.status = this }
        "OK"::localFunction
    }
    updateStatus()
}

fun testCase6(obj: TestData) = inlineFunction {
    class InlineClass

    val updateStatus: TestData.(String) -> Unit = inlineGenericTestFunction {
        fun TestData.localFunction(msg: String) { status = msg }
        TestData::localFunction
    }
    updateStatus(obj, "ERROR")
    obj.updateStatus("OK")
}

fun testCase7(obj: TestData) = inlineFunction {
    class InlineClass

    val updateStatus: (String) -> Unit = inlineGenericTestFunction {
        fun TestData.localFunction(msg: String) { status = msg }
        obj::localFunction
    }
    updateStatus("OK")
}

fun testCase8(obj: TestData) = inlineFunction {
    class InlineClass

    val updateStatus: (String) -> Unit = inlineGenericTestFunction {
        obj::updateStatus
    }
    updateStatus("OK")
}

fun testCase9(obj: TestData) = inlineFunction {
    class InlineClass

    val updateStatus: String.() -> Unit = inlineGenericTestFunction {
        { obj.updateStatus(this) }
    }
    updateStatus("ERROR")
    "OK".updateStatus()
}

fun testCase10(obj: TestData) = inlineFunction {
    class InlineClass

    val updateStatus: String.() -> Unit = inlineGenericTestFunction {
        fun <T> T.localFunction() { obj.status = this.toString() }
        String::localFunction
    }
    updateStatus("ERROR")
    "OK".updateStatus()
}

fun testCase11(obj: TestData) {
    fun testCaseImpl(msg: String) = inlineFunction {
        class InlineClass

        val updateStatus: (Boolean) -> Unit = inlineGenericTestFunction {
            fun TestData.localFunction(flag: Boolean) { status = if (flag) msg else "ERROR" }
            obj::localFunction
        }
        updateStatus(true)
    }
    testCaseImpl("OK")
}

fun testCase12(obj: TestData) {
    fun testCaseImpl(msg: String) = inlineFunction {
        class InlineClass

        val updateStatus: TestData.(Boolean) -> Unit = inlineGenericTestFunction {
            fun TestData.localFunction(flag: Boolean) { status = if (flag) msg else "ERROR" }
            TestData::localFunction
        }
        updateStatus(obj, false)
        obj.updateStatus(true)
    }
    testCaseImpl("OK")
}

fun testCase13(obj: TestData) {
    fun testCaseImpl(msg: String) = inlineFunction {
        class InlineClass

        val updateStatus: (Boolean) -> Unit = inlineGenericTestFunction {
            fun <T, F> T.localFunction(flag: F) { obj.status = if (flag!!.equals(true)) this.toString() else "ERROR" }
            msg::localFunction
        }
        updateStatus(false)
        updateStatus(true)
    }
    testCaseImpl("OK")
}

fun checkTest(test: (TestData) -> Unit): Boolean {
    val obj = TestData("Fail")
    test(obj)
    return obj.status == "OK"
}

fun box(): String {
    if (!checkTest(::testCase1)) return "Fail case 1"
    if (!checkTest(::testCase2)) return "Fail case 2"
    if (!checkTest(::testCase3)) return "Fail case 3"
    if (!checkTest(::testCase4)) return "Fail case 4"
    if (!checkTest(::testCase5)) return "Fail case 5"
    if (!checkTest(::testCase6)) return "Fail case 6"
    if (!checkTest(::testCase7)) return "Fail case 7"
    if (!checkTest(::testCase8)) return "Fail case 8"
    if (!checkTest(::testCase9)) return "Fail case 9"
    if (!checkTest(::testCase10)) return "Fail case 10"
    if (!checkTest(::testCase11)) return "Fail case 11"
    if (!checkTest(::testCase12)) return "Fail case 12"
    if (!checkTest(::testCase13)) return "Fail case 13"

    return "OK"
}
