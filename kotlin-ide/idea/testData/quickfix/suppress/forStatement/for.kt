// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for statement " "true"
// WITH_RUNTIME

fun foo() {
    for (i in list()<caret>!!) {}
}

fun list(): List<Int> = throw Exception()