// KIND: STANDALONE
// MODULE: ListExport
// FILE: main.kt

fun testListInt(l: List<Int>) = l
fun testListShort(l: List<Short>) = l
fun testListString(l: List<String>) = l
fun testListAny(l: List<Any>) = l

fun testListOptInt(l: List<Int?>) = l
fun testListOptString(l: List<String?>) = l
fun testListOptAny(l: List<Any?>) = l

fun testListListInt(l: List<List<Int>>) = l
fun testListOptListInt(l: List<List<Int>?>) = l
fun testOptListInt(l: List<Int>?) = l

fun testListNothing(l: List<Nothing>) = l
fun testListOptNothing(l: List<Nothing?>) = l
