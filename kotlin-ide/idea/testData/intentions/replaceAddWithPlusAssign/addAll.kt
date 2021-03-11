// WITH_RUNTIME
// INTENTION_TEXT: Replace 'addAll()' with '+='
fun foo() {
    val a = arrayListOf<Int>(1, 2, 3)
    a.<caret>addAll(arrayOf(4, 5, 6))
}
