// PROBLEM: none
fun test(str: String?): String? {
    val <caret>some = if (str != null) str + str else return null
    return when (some) {
        "some" -> some
        else -> ""
    }
}