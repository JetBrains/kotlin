// KIND: STANDALONE
// MODULE: SetExport
// FILE: main.kt

fun testSetInt(s: Set<Int>) = s
fun testSetShort(s: Set<Short>) = s
fun testSetString(s: Set<String>) = s
fun testSetAny(s: Set<Any>) = s

fun testSetOptInt(s: Set<Int?>) = s
fun testSetOptString(s: Set<String?>) = s
fun testSetOptAny(s: Set<Any?>) = s

fun testSetListInt(s: Set<List<Int>>) = s
fun testSetSetInt(s: Set<Set<Int>>) = s
fun testSetOptSetInt(s: Set<Set<Int>?>) = s
fun testOptSetInt(s: Set<Int>?) = s

fun testSetNothing(s: Set<Nothing>) = s
fun testSetOptNothing(s: Set<Nothing?>) = s
