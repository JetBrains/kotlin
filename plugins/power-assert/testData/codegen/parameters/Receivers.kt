// FUNCTION: Asserter.dispatchAssert
// FUNCTION: Asserter.memberExtensionAssert
// FUNCTION: extensionAssert
// DUMP_KT_IR

fun box(): String {
    return listOf(
        "test1: " to { test1() },
        "test2: " to { test2() },
        "test3: " to { test3() },
        "test4: " to { test4() },
        "test5: " to { test5() },
        "test6: " to { test6() },
    ).joinToString("") { (name, test) -> name + test() }
}

data object Asserter {
    fun dispatchAssert(condition: Boolean, msg: Any? = null) {
        if (!condition) throw AssertionError(msg.toString())
    }

    fun Asserter.memberExtensionAssert(condition: Boolean, msg: Any? = null) {
        if (!condition) throw AssertionError(msg.toString())
    }
}

fun Asserter.extensionAssert(condition: Boolean, msg: Any? = null) {
    if (!condition) throw AssertionError(msg.toString())
}

fun test1() = expectThrowableMessage {
    Asserter.dispatchAssert("test".length == 5)
}

fun test2() = expectThrowableMessage {
    with(Asserter) {
        dispatchAssert("test".length == 5)
    }
}

fun test3() = expectThrowableMessage {
    Asserter.extensionAssert("test".length == 5)
}

fun test4() = expectThrowableMessage {
    with(Asserter) {
        extensionAssert("test".length == 5)
    }
}

fun test5() = expectThrowableMessage {
    with(Asserter) {
        Asserter.memberExtensionAssert("test".length == 5)
    }
}

fun test6() = expectThrowableMessage {
    with(Asserter) {
        memberExtensionAssert("test".length == 5)
    }
}
