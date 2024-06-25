// FUNCTION: kotlin.assert
// FUNCTION: kotlin.requireNotNull
// DUMP_KT_IR

fun box(): String {
    return test1("test") +
            test1(1) +
            test2(1)
}

fun test1(a: Any) = expectThrowableMessage {
    assert((a as? String)?.length == 5)
}

fun test2(a: Any) = expectThrowableMessage {
    requireNotNull(a as? String) { "" }
}
