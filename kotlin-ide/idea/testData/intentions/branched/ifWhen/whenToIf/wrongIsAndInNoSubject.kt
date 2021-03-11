// ERROR: Expected condition of type Boolean
// ERROR: Expected condition of type Boolean
// SKIP_ERRORS_AFTER

fun test(n: Int): String {
    return <caret>when {
        is String -> "String"
        in 1..10 -> "1..10"
        else -> "unknown"
    }
}