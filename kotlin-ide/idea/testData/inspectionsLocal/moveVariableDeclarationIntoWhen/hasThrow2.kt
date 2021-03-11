// PROBLEM: none
fun test(str: String?): String? {
    val <caret>some = if (str != null) str + str else throw Exception()
    return when (some) {
        "some" -> some
        else -> ""
    }
}