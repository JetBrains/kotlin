// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +MultiDollarInterpolation

fun box(): String {
    return test1("Joe") +
            test2("Joe") +
            test3(listOf("a","b")) +
            test4("Joe") +
            test5() +
            test6("Joe")
}

fun test1(a: String) = expectThrowableMessage {
    assert("Hello, $a" == "string")
}

fun test2(a: String) = expectThrowableMessage {
    assert("${a.length}" == "5")
}

fun test3(a: List<String>) = expectThrowableMessage {
    assert("List: $a" == "")
}

fun test4(a: String) = expectThrowableMessage {
    assert("Escaping: \$ $a" == "")
}

fun test5() = expectThrowableMessage {
    assert("""Multiline escaping: ${'$'}_9.99 """ == "price")
}

fun test6(a: String) = expectThrowableMessage {
    assert($$"""Multidollar interpolation $a : $$a""" == "")
}