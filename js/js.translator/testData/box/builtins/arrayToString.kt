// This test's purpose is not preventing regressions, but rather making sure the array-to-string conversion is tested at all.
// The `expected` values in asserts just reflect the current state of things. It doesn't mean things _must_ be this way.
// So feel free to update them if you're sure that the array-to-string conversion should behave differently.
// See also: KT-14013
// WITH_STDLIB

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

    assertEquals("[...];[...];[...];", pullLog(), "testKt14013")
}

fun concretePrimitiveArrayToString(a: IntArray) {
    log(a.toString())
    log("$a")
    log("" + a)
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

    assertEquals("[...];[...];[...];", pullLog(), "concreteArrayToString")

    genericValueToString(a)

    assertEquals("[...];[...];[...];", pullLog(), "genericValueToString")

    anyValueToString(a)

    assertEquals("[...];[...];[...];", pullLog(), "anyValueToString")

    concretePrimitiveArrayToString(a.toIntArray())

    assertEquals("[...];[...];[...];", pullLog(), "concretePrimitiveArrayToString")

    return "OK"
}
