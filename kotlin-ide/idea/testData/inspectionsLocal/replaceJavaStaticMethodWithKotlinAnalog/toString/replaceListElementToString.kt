// WITH_RUNTIME

fun foo() {
    val list = arrayListOf<Int>()
    list.add(5)
    Integer.<caret>toString(list[0])
}
