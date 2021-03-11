operator fun String.get(a: Int, b: String, c: String): Int = 0

fun bar(b: String, a: Int, c: String) {
    ""[<caret>]
}

// EXIST: "a, b, c"
