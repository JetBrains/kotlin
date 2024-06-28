// EXPECTED_REACHABLE_NODES: 1282
// MODULE: #my_libr@ry
// MODULE_KIND: PLAIN
// FILE: bar.kt
// PROPERTY_NOT_WRITTEN_TO: baz
// PROPERTY_NOT_WRITTEN_TO: boo_287e2$
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

inline fun foo() = "foo"
@kotlin.internal.InlineOnly
inline fun baz() = "baz"
inline fun <reified T> boo() = "boo"

// MODULE: main(#my_libr@ry)
// MODULE_KIND: PLAIN
// FILE: box.kt
// CHECK_CONTAINS_NO_CALLS: box except=assertEquals

fun box(): String {
    assertEquals("foo", foo())
    assertEquals("baz", baz())
    assertEquals("boo", boo<Int>())
    return "OK"
}
