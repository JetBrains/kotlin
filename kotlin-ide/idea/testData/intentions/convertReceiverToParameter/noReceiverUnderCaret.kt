// IS_APPLICABLE: false
fun String.foo<caret>(n: Int): Boolean {
    return length - n/2 > 1
}