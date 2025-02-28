fun box(): String {
    return "abc".test1() + null.test1()
}

fun String?.test1() = expectThrowableMessage {
    assert(this?.length == 5)
}
