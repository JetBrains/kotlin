fun test(n: Int): String? {
    var res<caret> = when (n) {
        1 -> "one"
        2 -> "two"
        else -> null
    }

    return res
}