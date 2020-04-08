fun test(n: Int): String? {
    val res<caret>: String? = when (n) {
        1 -> "one"
        2 -> "two"
        else -> null
    }

    return res
}