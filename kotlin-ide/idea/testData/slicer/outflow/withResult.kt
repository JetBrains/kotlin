// FLOW: OUT
// RUNTIME_WITH_SOURCES

fun String.foo(<caret>p: String) {
    val v1 = with(p) { this }

    val v2 = with(p) { bar(this) }

    val v3 = with(p) { this@foo }

    val v4 = with(p, ::zoo)
}

fun bar(s: String) = s
fun zoo(s: String) = s
