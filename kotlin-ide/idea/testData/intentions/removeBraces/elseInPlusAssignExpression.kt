// WITH_RUNTIME
fun test(b: Boolean) {
    val list1 = mutableListOf(1)
    val list2 = mutableListOf(2)

    if (b) {list1} else {list2}<caret> += 3
}