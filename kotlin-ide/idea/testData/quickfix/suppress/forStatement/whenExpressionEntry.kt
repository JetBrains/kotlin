// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for statement " "true"

fun foo(a: Any) {
    when (a) {
        ""<caret>!! -> {}
    }
}