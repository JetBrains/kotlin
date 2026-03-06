// FUNCTION: kotlin.requireNotNull
// FUNCTION: kotlin.checkNotNull
// FUNCTION: expectAny
// FUNCTION: expectAnyNullable
// FUNCTION: expectCount

fun box(): String {
    return test1(1) +
            test2(1) +
            test3(1) +
            test4("") +
            test5(1)
}

fun test1(a: Any) = expectThrowableMessage {
    requireNotNull(a as? String)
}

fun test2(a: Any) = expectThrowableMessage {
    checkNotNull(a as? String)
}

fun test3(a: Any) = expectThrowableMessage {
    expectAny(a as? String)
}

fun test4(a: Any) = expectThrowableMessage {
    expectAnyNullable(a as String)
}

fun test5(a: Any) = expectThrowableMessage {
    expectCount(a as? String)
}

fun <T> expectAny(value: T): Any? = error("OK")
fun <T : Any> expectAny(value: T, msg: String): Any? = error("! BAD !")

fun <T : Any> expectAnyNullable(value: T): Any? = error("! BAD !")
fun <T> expectAnyNullable(value: T, msg: String): Any? = error("OK")

fun <T, R : T> expectCount(value: R): Any? = error("OK")
fun <T> expectCount(value: T, msg: String): Any? = error("! BAD !")