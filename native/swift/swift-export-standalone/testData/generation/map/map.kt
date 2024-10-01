// KIND: STANDALONE
// MODULE: MapExport
// FILE: main.kt

fun testMapIntString(m: Map<Int, String>) = m
fun testMapStringInt(m: Map<String, Int>) = m

fun testMapLongAny(m: Map<Long, Any>) = m
fun testMapAnyLong(m: Map<Any, Long>) = m

fun testMapOptIntListInt(m: Map<Int?, List<Int>>) = m
fun testMapListIntSetInt(m: Map<List<Int>, Set<Int>>) = m
fun testMapSetIntMapIntInt(m: Map<Set<Int>, Map<Int, Int>>) = m

fun testMapNothingOptNothing(m: Map<Nothing, Nothing?>) = m
fun testMapOptNothingNothing(m: Map<Nothing?, Nothing>) = m
