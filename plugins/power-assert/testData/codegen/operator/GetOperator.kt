fun box(): String {
    return test1() +
            test2()
}

fun test1() = expectThrowableMessage {
    assert(listOf("Hello", "World")[1] == "Hello")
}

fun test2() = expectThrowableMessage {
    assert(listOf("Hello", "World").get(1) == "Hello")
}
