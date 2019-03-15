// KJS_WITH_FULL_RUNTIME
// EXPECTED_REACHABLE_NODES: 1515
// KT-3518 Null pointer during null comparison in JS Backend
package foo

class MyClazz(val nullableL : List<String>?)

fun box(): String {
    val a = MyClazz(null)
    if(a.nullableL != null) return "a.nullableL != null"

    val b = MyClazz(listOf("somthing"))
    if(b.nullableL == null) return "b.nullableL == null"

    return "OK"
}