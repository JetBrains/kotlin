// FUNCTION: powerAssert
// DUMP_KT_IR

fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2() },
    "test3" to { test3() },
)

fun powerAssert(condition: Boolean, msg: String? = null) {
    if (!condition) throw AssertionError(msg?.toString() ?: "Assertion failed")
}

fun powerAssert(condition: Boolean, msg: () -> String) {
    if (!condition) throw AssertionError(msg.invoke())
}

fun test1() {
    powerAssert("test".length == 5)
}

fun test2() {
    powerAssert("test".length == 5, msg = "bad")
}

fun test3() {
    powerAssert("test".length == 5, msg = { "bad" })
}
