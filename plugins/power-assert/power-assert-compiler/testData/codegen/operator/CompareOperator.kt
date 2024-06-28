// ISSUE: KT-73871

fun box(): String {
    return listOf(
        "test1: " to { test1() },
        "test2: " to { test2() },
        "test3: " to { test3() },
        "test4: " to { test4() },
        "test5: " to { test5() },
        "test6: " to { test6() },
        "test7: " to { test7() },
        "test8: " to { test8() },
    ).joinToString("") { (name, test) -> name + test() }
}

fun test1() = expectThrowableMessage {
    assert("aaa".compareTo("bbb") > 0)
}

fun test2() = expectThrowableMessage {
    assert("aaa" > "bbb")
}

fun test3() = expectThrowableMessage {
    assert("aaa".compareTo("bbb") >= 0)
}

fun test4() = expectThrowableMessage {
    assert("aaa" >= "bbb")
}

fun test5() = expectThrowableMessage {
    assert("bbb".compareTo("aaa") < 0)
}

fun test6() = expectThrowableMessage {
    assert("bbb" < "aaa")
}

fun test7() = expectThrowableMessage {
    assert("bbb".compareTo("aaa") <= 0)
}

fun test8() = expectThrowableMessage {
    assert("bbb" <= "aaa")
}
