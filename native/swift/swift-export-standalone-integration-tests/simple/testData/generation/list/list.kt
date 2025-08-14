// KIND: STANDALONE
// MODULE: ListExport
// EXPORT_TO_SWIFT
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

fun testStarList(l: List<*>) = l

// MODULE: ListExport_2
// EXPORT_TO_SWIFT
// FILE: main_2.kt

package list2

interface MyList<out T>: List<T> {

}

//typealias MyList<T> = List<T>

fun testListInt(l: MyList<Int>) = l
fun testListShort(l: MyList<Short>) = l
fun testListString(l: MyList<String>) = l
fun testListAny(l: MyList<Any>) = l

fun testListOptInt(l: MyList<Int?>) = l
fun testListOptString(l: MyList<String?>) = l
fun testListOptAny(l: MyList<Any?>) = l

fun testListListInt(l: MyList<MyList<Int>>) = l
fun testListOptListInt(l: MyList<MyList<Int>?>) = l
fun testOptListInt(l: MyList<Int>?) = l

fun testListNothing(l: MyList<Nothing>) = l
fun testListOptNothing(l: MyList<Nothing?>) = l

fun testStarList(l: MyList<*>) = l
