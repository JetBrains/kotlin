// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY
// FUNCTION: Wrapper.mustEqual

data object Asserter

class Wrapper<V>(
    private val value: V,
) {
    context(_: Asserter)
    infix fun mustEqual(expected: V): Unit = assert(value == expected)

    context(_: Asserter)
    fun mustEqual(expected: V, message: () -> String): Unit =
        assert(value == expected, message)

    override fun toString() = "Wrapper"
}

data class Holder<T>(val wrapper: Wrapper<T>)

data object Complex {
    val holder = Holder(Wrapper(3))
}

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
        Wrapper(2) mustEqual 6
    }
}

fun test2() = expectThrowableMessage {
    with(Asserter) {
        Wrapper(1) mustEqual (2 + 4)
    }
}

fun test3() = expectThrowableMessage {
    with(Asserter) {
        Wrapper(1 + 1) mustEqual 6
    }
}

fun test4() = expectThrowableMessage {
    with(Asserter) {
        Wrapper(1 + 1) mustEqual (2 + 4)
    }
}

fun test5() = expectThrowableMessage {
    with(Asserter) {
        Complex.holder.wrapper mustEqual "world".length
    }
}

// ============ //
// Regular Call //
// ============ //

fun test6() = expectThrowableMessage {
    with(Asserter) {
        Wrapper(2).mustEqual(6)
    }
}

fun test7() = expectThrowableMessage {
    with(Asserter) {
        Wrapper(1).mustEqual(2 + 4)
    }
}

fun test8() = expectThrowableMessage {
    with(Asserter) {
        Wrapper(1 + 1).mustEqual(6)
    }
}

fun test9() = expectThrowableMessage {
    with(Asserter) {
        Wrapper(1 + 1).mustEqual(2 + 4)
    }
}

fun test10() = expectThrowableMessage {
    with(Asserter) {
        Complex.holder.wrapper.mustEqual("world".length)
    }
}
