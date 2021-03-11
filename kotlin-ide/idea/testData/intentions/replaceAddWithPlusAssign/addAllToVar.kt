// IS_APPLICABLE: false
// WITH_RUNTIME

fun foo() {
    var a = arrayListOf<Int>(1, 2, 3)
    a.<caret>addAll(arrayListOf(4))
}
