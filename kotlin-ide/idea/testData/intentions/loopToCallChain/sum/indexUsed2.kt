// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'mapIndexed{}.sum()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().mapIndexed{}.sum()'"
fun foo(list: List<Any>): Int {
    var s = 0
    <caret>for ((index, item) in list.withIndex()) {
        s += getShort(index, item)
    }
    return s
}

fun getShort(index: Int, item: Any): Short = TODO()
