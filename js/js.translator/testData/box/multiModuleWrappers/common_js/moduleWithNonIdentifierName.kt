// EXPECTED_REACHABLE_NODES: 1285
// MODULE: module_1
// MODULE_KIND: COMMON_JS
// FILE: bar.kt
fun bar() = "bar"

// MODULE: main(module_1)
// MODULE_KIND: COMMON_JS
// FILE: box.kt
fun box(): String {
    assertEquals("bar", bar())
    return "OK"
}
