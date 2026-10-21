// KIND: STANDALONE
// MODULE: UnboundGenerics
// FILE: Describe.kt
// Shared Kotlin-side probes: tell Swift what Kotlin actually received through Any / T.

class Foo(val id: Int)
enum class Letter { A, B }

fun describeAny(x: Any?): String = when (x) {
    null -> "null"
    is String -> "String:$x"
    is Byte -> "Byte:$x"
    is Short -> "Short:$x"
    is Int -> "Int:$x"
    is Long -> "Long:$x"
    is UByte -> "UByte:$x"
    is UShort -> "UShort:$x"
    is UInt -> "UInt:$x"
    is ULong -> "ULong:$x"
    is Float -> "Float:$x"
    is Double -> "Double:$x"
    is Boolean -> "Boolean:$x"
    is Letter -> "Enum:${x.name}"
    is List<*> -> "List[" + x.joinToString(",") { describeAny(it) } + "]"
    is Set<*> -> "Set[" + x.map { describeAny(it) }.sorted().joinToString(",") + "]"
    is Map<*, *> -> "Map[" + x.entries.map { describeAny(it.key) + "=" + describeAny(it.value) }.sorted().joinToString(",") + "]"
    else -> "Other"
}

fun <T> describeT(x: T): String = describeAny(x)

// FILE: Collections.kt
// What Kotlin sees for Swift collections passed as Any.

fun firstIsFoo(x: Any): Boolean = (x as? List<*>)?.firstOrNull() is Foo
fun firstFooId(x: Any): Int = ((x as? List<*>)?.firstOrNull() as? Foo)?.id ?: -1

fun <T> describeList(items: List<T>): String = items.joinToString(",") { describeAny(it) }

// FILE: Strings.kt
// empty strings through Any, both directions.

fun anyEmpty(): Any = ""
fun isEmpty(x: Any): Boolean = x == ""

// FILE: Casts.kt
// Kotlin-side `as?` and toString() on a value received through T.

fun <T> castToString(x: T): String? = x as? String
fun <T> castToInt(x: T): Int? = x as? Int
fun <T> genericToString(x: T): String = x.toString()
