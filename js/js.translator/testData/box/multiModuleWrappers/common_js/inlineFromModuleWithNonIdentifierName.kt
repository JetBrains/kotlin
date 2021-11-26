// EXPECTED_REACHABLE_NODES: 1284
// MODULE: #my_libr@ry
// MODULE_KIND: COMMON_JS
// FILE: bar.kt
// PROPERTY_NOT_WRITTEN_TO: baz
// PROPERTY_NOT_WRITTEN_TO: boo_287e2$
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

inline fun foo() = "foo"
@kotlin.internal.InlineOnly
inline fun baz() = "baz"
inline fun <reified T> boo() = "boo"

// MODULE: main(#my_libr@ry)
// MODULE_KIND: COMMON_JS
// FILE: box.kt
// CHECK_CONTAINS_NO_CALLS: box except=assertEquals;assertEquals$default

fun box(): String {
    assertEquals("foo", foo())
    assertEquals("baz", baz())
    assertEquals("boo", boo<Int>())
    return "OK"
}
