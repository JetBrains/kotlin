// DUMP_KT_IR

fun box(): String {
    return test1("TEST", 0) +
            test1(null, 0) +
            test2("TEST", null, 0) +
            test2(null, "test", 0) +
            test2(null, null, 0)
}

fun test1(str: String?, default: Int) = expectThrowableMessage {
    assert((str?.lowercase()?.length ?: default) == 5)
}

fun test2(str: String?, fallback: String?, default: Int) = expectThrowableMessage {
    assert(((str?.lowercase() ?: fallback)?.length ?: default) == 5)
}
