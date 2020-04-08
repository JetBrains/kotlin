// WITH_RUNTIME
// INTENTION_TEXT: Replace 'add()' with '+='
fun foo() {
    val a = arrayListOf<Int>(1, 2, 3)
    a.<caret>add(4)
}
