// EXPECTED_REACHABLE_NODES: 492
package foo

class T4(
        val c1: Boolean,
        val c2: Boolean,
        val c3: Boolean,
        val c4: String
) {
    override fun equals(o: Any?): Boolean {
        if (o !is T4) return false;
        return c1 == o.c1 &&
            c2 == o.c2 &&
            c3 == o.c3 &&
            c4 == o.c4
    }
}

fun reformat(
        str: String,
        normalizeCase: Boolean = true,
        uppercaseFirstLetter: Boolean = true,
        divideByCamelHumps: Boolean = true,
        wordSeparator: String = " "
) =
        T4(normalizeCase, uppercaseFirstLetter, divideByCamelHumps, wordSeparator)


fun box(): String {
    val expected = T4(true, true, true, " ")
    if (reformat("", true, true, true, " ") != expected) return "fail1"
    if (reformat("", true, true, true) != expected) return "fail2"
    if (reformat("", true, true) != expected) return "fail3"
    if (reformat("", true) != expected) return "fail4"
    if (reformat("") != expected) return "fail5"
    return "OK"
}
