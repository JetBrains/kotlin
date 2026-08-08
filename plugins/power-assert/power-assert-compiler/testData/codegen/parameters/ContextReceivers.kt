// LANGUAGE: +ContextParameters
// FUNCTION: context1Assert
// FUNCTION: context2Assert
// DUMP_KT_IR

// ISSUE: KT-88179

fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2() },
    "test3" to { test3() },
    "test4" to { test4() },
)

data object Asserter

context(_: Asserter)
fun context1Assert(condition: Boolean, msg: Any? = null) {
    if (!condition) throw AssertionError(msg.toString())
}

context(_: Asserter, _: Asserter)
fun context2Assert(condition: Boolean, msg: Any? = null) {
    if (!condition) throw AssertionError(msg.toString())
}

fun test1() {
    with(Asserter) {
        context1Assert("test".length == 5)
    }
}

fun test2() {
    context(Asserter) {
        context1Assert("test".length == 5)
    }
}

fun test3() {
    with(Asserter) {
        context2Assert("test".length == 5)
    }
}

fun test4() {
    context(Asserter) {
        context2Assert("test".length == 5)
    }
}
