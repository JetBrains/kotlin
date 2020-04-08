// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for statement " "true"

fun foo(a: Array<Int>) {
    a[1<caret>!!]
}