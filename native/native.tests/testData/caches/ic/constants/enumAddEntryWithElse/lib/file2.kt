package test

fun name(e: E): String = when (e) {
    E.A -> "a"
    E.B -> "b"
    else -> "other"
}
