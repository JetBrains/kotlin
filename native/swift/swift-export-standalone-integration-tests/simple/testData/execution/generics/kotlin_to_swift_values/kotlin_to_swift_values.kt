// KIND: STANDALONE
// MODULE: UnboundGenerics
// FILE: Describe.kt
// Shared Kotlin-side probes: tell Swift what Kotlin actually received through Any / T.

class Foo(val id: Int)

fun describeAny(x: Any?): String = when (x) {
    is Foo -> "Foo:${x.id}"
    else -> "Other"
}

// FILE: CollectionContents.kt
// Element types inside collections returned as Any.

fun anyListOfInt(): Any = listOf(1, 2)
fun anyListOfString(): Any = listOf("a", "b")
fun anyListOfFoo(): Any = listOf(Foo(1), Foo(2))
fun anyNestedList(): Any = listOf(listOf(1, 2), listOf(3))
fun anyListWithNull(): Any = listOf(1, null, 3)
fun anyEmptyList(): Any = emptyList<Int>()

fun anySetOfString(): Any = setOf("a")
fun anySetOfInt(): Any = setOf(1, 2)
fun anyEmptySet(): Any = emptySet<String>()

fun anyMapOfStringToFoo(): Any = mapOf("k" to Foo(3))
fun anyMapOfStringToInt(): Any = mapOf("a" to 1, "b" to 2)
fun anyMapWithNullKey(): Any = mapOf(null to 1, "a" to 2)
fun anyMapOfStringToString(): Any = mapOf("key" to "value")
fun anyMapWithNullValue(): Any = mapOf("a" to null)
fun anyMapOfListValues(): Any = mapOf("xs" to listOf(1, 2))
fun anyEmptyMap(): Any = emptyMap<String, Int>()

// FILE: TagZero.kt
// Values outside the tag list (tag 0) stay wrapped; String (tag 1) and numeric tags 2-12 unbox to native Swift types.

fun anyFoo(): Any = Foo(42)

// An exported interface whose implementing class is not exported: Swift has no class wrapper for it,
// so the value comes back through the protocol-wrapper path.
interface Shape { fun area(): Int }
private class HiddenSquare(val side: Int) : Shape { override fun area(): Int = side * side }
fun anyShape(): Any = HiddenSquare(3)

fun anyString(): Any = "hello"
fun anyByte(): Any = 1.toByte()
fun anyShort(): Any = 2.toShort()
fun anyInt(): Any = 3
fun anyLong(): Any = 4L
fun anyUByte(): Any = 5u.toUByte()
fun anyUShort(): Any = 6u.toUShort()
fun anyUInt(): Any = 7u
fun anyULong(): Any = 8uL
fun anyFloat(): Any = 9.5f
fun anyDouble(): Any = 10.25
fun anyBoolean(): Any = true
fun uLongMax(): Any = ULong.MAX_VALUE

// FILE: CollectionIdentity.kt
// A Kotlin class implementing List (not exported itself) still hits tag 13.
class KotlinList : AbstractList<Int>() {
    override val size: Int get() = 2
    override fun get(index: Int): Int = index * 10
}

fun customList(): Any = KotlinList()
