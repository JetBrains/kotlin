// EXPECTED_REACHABLE_NODES: 1284
// MODULE: module_1
// FILE: bar.kt
// MODULE_KIND: AMD
fun bar() = "bar"

// MODULE: main(module_1)
// FILE: box.kt
// MODULE_KIND: AMD
fun box(): String {
    assertEquals("bar", bar())
    return "OK"
}