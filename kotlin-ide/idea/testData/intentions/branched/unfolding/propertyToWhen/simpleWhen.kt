fun test(n: Int): String? {
    val res<caret> = when (n) {
        1 -> "one"
        2 -> "two"
        else -> null
    }

    return res
}