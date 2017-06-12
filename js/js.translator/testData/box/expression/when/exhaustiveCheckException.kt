// EXPECTED_REACHABLE_NODES: 555
fun <T> checkThrown(x: T, block: (T) -> Any?): Unit? {
    return try {
        println((block(x) ?: "").toString())
        null
    }
    catch (e: NoWhenBranchMatchedException) {
        Unit
    }
}

fun <T> checkNotThrown(x: T, block: (T) -> Any?): Unit? {
    return try {
        println((block(x) ?: "").toString())
        Unit
    }
    catch (e: NoWhenBranchMatchedException) {
        null
    }
}

sealed class C {
    class X : C()

    class Y : C()
}

enum class E {
    X, Y
}

private inline fun createWrongC(): C = js("void 0").unsafeCast<C>()

private inline fun createWrongE(): E = js("void 0").unsafeCast<E>()

fun box(): String {
    checkThrown(createWrongC()) {
        when (it) {
            is C.X -> 0
            is C.Y -> 1
        }
    } ?: return "fail1"

    checkNotThrown(createWrongC()) {
        when (it) {
            is C.X -> 0
            else -> 1
        }
    } ?: return "fail2"

    checkThrown(createWrongE()) {
        when (it) {
            E.X -> 0
            E.Y -> 1
        }
    } ?: return "fail3"

    checkNotThrown(createWrongE()) {
        when (it) {
            E.X -> 0
            else -> 1
        }
    } ?: return "fail4"

    checkNotThrown(createWrongC()) {
        when (it) {
            is C.X -> {}
            is C.Y -> {}
        }
        Unit
    } ?: return "fail5"

    checkNotThrown(createWrongC()) {
        when (it) {
            is C.X -> {}
            is C.Y -> {}
        }
    } ?: return "fail6"

    checkNotThrown(createWrongC()) {
        when (it) {
            is C.X -> Unit
            is C.Y -> Unit
        }
    } ?: return "fail7"

    checkThrown(createWrongC()) {
        when (it) {
            is C.X -> Unit
            is C.Y -> null as Unit?
        }
    } ?: return "fail8"

    return "OK"
}