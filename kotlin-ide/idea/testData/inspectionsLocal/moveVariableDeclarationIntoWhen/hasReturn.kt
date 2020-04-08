// PROBLEM: none
fun test(str: String?): String? {
    val <caret>some = str ?: return null
    return when (some) {
        "some" -> some
        else -> ""
    }
}