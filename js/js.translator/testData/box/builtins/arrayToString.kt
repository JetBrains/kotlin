// EXPECTED_REACHABLE_NODES: 1277

// This test's purpose is not preventing regressions, but rather making sure the array-to-string conversion is tested at all.
// The `expected` values in asserts just reflect the current state of things. It doesn't mean things _must_ be this way.
// So feel free to update them if you're sure that the array-to-string conversion should behave differently.
// See also: KT-14013

var LOG = ""

fun log(message: Any?) {
    LOG += message
    LOG += ";"
}

fun pullLog(): String {
    val string = LOG
    LOG = ""
    return string
}

fun testKt14013() {
    val a: Any? = arrayOf(null, 1)
    log(a)
    log(a.toString())
    log(a!!.toString())

    if (testUtils.isLegacyBackend()) {
        assertEquals(",1;[...];,1;", pullLog(), "testKt14013")
    } else {
        assertEquals("[...];[...];[...];", pullLog(), "testKt14013")
    }
}

fun concreteArrayToString(a: Array<Int>) {
    log(a.toString())
    log("$a")
    log("" + a)
}

fun <T> genericValueToString(a: T) {
    log(a.toString())
    log("$a")
    log("" + a)
}

fun anyValueToString(a: Any) {
    log(a.toString())
    log("$a")
    log("" + a)
}

fun box(): String {
    testKt14013()

    val a = arrayOf(1, 2, 3)
    concreteArrayToString(a)

    if (testUtils.isLegacyBackend()) {
        assertEquals("1,2,3;1,2,3;[...];", pullLog(), "concreteArrayToString")
    } else {
        assertEquals("[...];1,2,3;1,2,3;", pullLog(), "concreteArrayToString")
    }

    genericValueToString(a)

    if (testUtils.isLegacyBackend()) {
        assertEquals("[...];1,2,3;[...];", pullLog(), "genericValueToString")
    } else {
        assertEquals("[...];[...];[...];", pullLog(), "genericValueToString")
    }

    anyValueToString(a)

    if (testUtils.isLegacyBackend()) {
        assertEquals("1,2,3;1,2,3;[...];", pullLog(), "anyValueToString")
    } else {
        assertEquals("[...];[...];[...];", pullLog(), "anyValueToString")
    }

    return "OK"
}
