// IS_APPLICABLE: false
fun test(i: Int) {
    val p: (String) -> Boolean =
        if (i == 1) { s -> true } else { <caret>s -> false }
}