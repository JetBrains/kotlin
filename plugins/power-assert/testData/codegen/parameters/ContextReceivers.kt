// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY
// FUNCTION: context1Assert
// FUNCTION: context2Assert
// DUMP_KT_IR

fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2() },
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
    with(Asserter) {
        context2Assert("test".length == 5)
    }
}
