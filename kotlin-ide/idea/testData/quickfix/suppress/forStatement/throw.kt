// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for statement " "true"
// WITH_RUNTIME

fun foo(): Any {
    throw Exception(""<caret>!!)
}