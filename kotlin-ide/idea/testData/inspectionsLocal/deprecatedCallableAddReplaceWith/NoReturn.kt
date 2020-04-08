// PROBLEM: none
// ERROR: A 'return' expression required in a function with a block body ('{...}')

<caret>@Deprecated("")
fun foo(): String {
    bar()
}

fun bar(): String = ""