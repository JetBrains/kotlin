// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-73870, KT-73898

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
        "test11: " to { test11() },
        "test12: " to { test12() },
        "test13: " to { test13() },
        "test14: " to { test14() },
        "test15: " to { test15() },
        "test16: " to { test16() },
        "test17: " to { test17() },
        "test18: " to { test18() },
        "test19: " to { test19() },
        "test20: " to { test20() },
        "test21: " to { test21() },
        "test22: " to { test22() },
        "test23: " to { test23() },
        "test24: " to { test24() },
    ).joinToString("") { (name, test) -> name + test() }
}

// ======= //
// Helpers //
// ======= //

sealed interface Argument
data object Failure : Argument
data class Context(val n: Int) : Argument
data class Extension(val n: Int) : Argument

data class Dispatch(val n: Int) : Argument {
    operator fun plus(other: Dispatch): Argument = Dispatch(n + other.n)

    operator fun contains(other: Dispatch): Boolean = false

    operator fun Extension.minus(other: Extension): Argument = Extension(n - other.n)

    operator fun Extension.contains(other: Extension): Boolean = false

    context(_: Context)
    operator fun minus(other: Dispatch): Argument = Dispatch(n - other.n)

    context(_: Context)
    operator fun contains(other: Context): Boolean = false

    context(_: Context)
    operator fun Extension.times(other: Extension): Argument = Extension(n * other.n)

    context(_: Context)
    operator fun Extension.contains(other: Context): Boolean = false
}

operator fun Extension.plus(other: Extension): Argument = Extension(n + other.n)

operator fun Extension.contains(other: Dispatch): Boolean = false

context(_: Context)
operator fun Extension.minus(other: Extension): Argument = Extension(n + other.n)

context(_: Context)
operator fun Extension.contains(other: Dispatch): Boolean = false

val dispatch = Dispatch(1)
val extension = Extension(1)
val context = Context(1)

// ============= //
// Operator Call //
// ============= //

fun test1() = expectThrowableMessage {
    assert(dispatch + dispatch == Failure)
}

fun test2() = expectThrowableMessage {
    assert(extension + extension == Failure)
}

fun test3() = expectThrowableMessage {
    with(dispatch) {
        assert(extension - extension == Failure)
    }
}

fun test4() = expectThrowableMessage {
    with(context) {
        assert(dispatch - dispatch == Failure)
    }
}

fun test5() = expectThrowableMessage {
    with(context) {
        assert(extension - extension == Failure)
    }
}

fun test6() = expectThrowableMessage {
    with(dispatch) {
        with(context) {
            assert(extension * extension == Failure)
        }
    }
}

fun test7() = expectThrowableMessage {
    assert(dispatch in dispatch)
}

fun test8() = expectThrowableMessage {
    assert(dispatch in extension)
}

fun test9() = expectThrowableMessage {
    with(dispatch) {
        assert(extension in extension)
    }
}

fun test10() = expectThrowableMessage {
    with(context) {
        assert(context in dispatch)
    }
}

fun test11() = expectThrowableMessage {
    with(context) {
        assert(dispatch in extension)
    }
}

fun test12() = expectThrowableMessage {
    with(dispatch) {
        with(context) {
            assert(context in extension)
        }
    }
}

// ============ //
// Regular Call //
// ============ //

fun test13() = expectThrowableMessage {
    assert(dispatch.plus(dispatch) == Failure)
}

fun test14() = expectThrowableMessage {
    assert(extension.plus(extension) == Failure)
}

fun test15() = expectThrowableMessage {
    with(dispatch) {
        assert(extension.minus(extension) == Failure)
    }
}

fun test16() = expectThrowableMessage {
    with(context) {
        assert(dispatch.minus(dispatch) == Failure)
    }
}

fun test17() = expectThrowableMessage {
    with(context) {
        assert(extension.minus(extension) == Failure)
    }
}

fun test18() = expectThrowableMessage {
    with(dispatch) {
        with(context) {
            assert(extension.times(extension) == Failure)
        }
    }
}

fun test19() = expectThrowableMessage {
    assert(dispatch.contains(dispatch))
}

fun test20() = expectThrowableMessage {
    assert(extension.contains(dispatch))
}

fun test21() = expectThrowableMessage {
    with(dispatch) {
        assert(extension.contains(extension))
    }
}

fun test22() = expectThrowableMessage {
    with(context) {
        assert(dispatch.contains(context))
    }
}

fun test23() = expectThrowableMessage {
    with(context) {
        assert(extension.contains(dispatch))
    }
}

fun test24() = expectThrowableMessage {
    with(dispatch) {
        with(context) {
            assert(extension.contains(context))
        }
    }
}
