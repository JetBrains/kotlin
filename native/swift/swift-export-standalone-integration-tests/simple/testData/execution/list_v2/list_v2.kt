// KIND: STANDALONE
// MODULE: ListExport
// SWIFT_EXPORT_CONFIG: collectionsV2=true
// FILE: main.kt

class Box(val x: Int)

fun listOf(vararg elements: Int): List<Int> = elements.asList()
fun reverseListInt(l: List<Int>) = l.reversed()

fun listOf(vararg elements: Short): List<Short> = elements.asList()
fun reverseListShort(l: List<Short>) = l.reversed()

fun listOf(vararg elements: Char): List<Char> = elements.asList()
fun reverseListChar(l: List<Char>) = l.reversed()

fun listOf(vararg elements: String): List<String> = elements.asList()
fun reverseListString(l: List<String>) = l.reversed()

fun listOf(vararg elements: Box): List<Box> = elements.asList()
fun reverseListBox(l: List<Box>) = l.reversed()

fun listOf(vararg elements: Int?): List<Int?> = elements.asList()
fun reverseListOptInt(l: List<Int?>) = l.reversed()

fun listOf(vararg elements: String?): List<String?> = elements.asList()
fun reverseListOptString(l: List<String?>) = l.reversed()

fun listOf(vararg elements: Box?): List<Box?> = elements.asList()
fun reverseListOptBox(l: List<Box?>) = l.reversed()

fun listOf(vararg elements: List<Int>): List<List<Int>> = elements.asList()
fun reverseListListInt(l: List<List<Int>>) = l.reversed()

fun listOf(vararg elements: List<Int>?): List<List<Int>?> = elements.asList()
fun reverseListOptListInt(l: List<List<Int>?>) = l.reversed()

fun reverseOptListInt(l: List<Int>?) = l?.reversed()

fun reverseListNothing(l: List<Nothing>) = l.reversed()
fun reverseListOptNothing(l: List<Nothing?>) = l.reversed()

fun List<Int>.extReverseListInt() = this.reversed()

val List<Int>.extReverseListIntProp
        get() = this.reversed()

fun mutableListOf(vararg elements: Int): MutableList<Int> = elements.toMutableList()
