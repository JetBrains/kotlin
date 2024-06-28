// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY
// FUNCTION: mustEqual

data object Asserter

context(_: Asserter)
infix fun <V> V.mustEqual(expected: V): Unit = assert(this == expected)

context(_: Asserter)
fun <V> V.mustEqual(expected: V, message: () -> String): Unit =
    assert(this == expected, message)

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
        "test9: " to { test9() },
        "test10: " to { test10() },
    ).joinToString("") { (name, test) -> name + test() }
}

// ========== //
// Infix Call //
// ========== //

fun test1() = expectThrowableMessage {
    with(Asserter) {
        2 mustEqual 6
    }
}

fun test2() = expectThrowableMessage {
    with(Asserter) {
        1 mustEqual (2 + 4)
    }
}

fun test3() = expectThrowableMessage {
    with(Asserter) {
        (1 + 1) mustEqual 6
    }
}

fun test4() = expectThrowableMessage {
    with(Asserter) {
        (1 + 1) mustEqual (2 + 4)
    }
}

fun test5() = expectThrowableMessage {
    with(Asserter) {
        "hello".substring(1, 4).length mustEqual "world".length
    }
}

// ============ //
// Regular Call //
// ============ //

fun test6() = expectThrowableMessage {
    with(Asserter) {
        2.mustEqual(6)
    }
}

fun test7() = expectThrowableMessage {
    with(Asserter) {
        1.mustEqual(2 + 4)
    }
}

fun test8() = expectThrowableMessage {
    with(Asserter) {
        (1 + 1).mustEqual(6)
    }
}

fun test9() = expectThrowableMessage {
    with(Asserter) {
        (1 + 1).mustEqual(2 + 4)
    }
}

fun test10() = expectThrowableMessage {
    with(Asserter) {
        "hello".substring(1, 4).length.mustEqual("world".length)
    }
}
