package foo

fun <T> T.toPrefixedString(prefix: String = "", suffix: String = "") = prefix + toString() + suffix

fun box(): Boolean {
    return ("mama".toPrefixedString(suffix = "321", prefix = "papa") == "papamama321")
}
