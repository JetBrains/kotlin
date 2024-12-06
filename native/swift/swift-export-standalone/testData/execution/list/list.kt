// KIND: STANDALONE
// MODULE: ListExport
// FILE: main.kt

class Box(val x: Int)

fun reverseListInt(l: List<Int>) = l.reversed()
fun reverseListShort(l: List<Short>) = l.reversed()
fun reverseListChar(l: List<Char>) = l.reversed()
fun reverseListString(l: List<String>) = l.reversed()
fun reverseListBox(l: List<Box>) = l.reversed()

fun reverseListOptInt(l: List<Int?>) = l.reversed()
fun reverseListOptString(l: List<String?>) = l.reversed()
fun reverseListOptBox(l: List<Box?>) = l.reversed()

fun reverseListListInt(l: List<List<Int>>) = l.reversed()
fun reverseListOptListInt(l: List<List<Int>?>) = l.reversed()
fun reverseOptListInt(l: List<Int>?) = l?.reversed()

fun reverseListNothing(l: List<Nothing>) = l.reversed()
fun reverseListOptNothing(l: List<Nothing?>) = l.reversed()

fun List<Int>.extReverseListInt() = this.reversed()