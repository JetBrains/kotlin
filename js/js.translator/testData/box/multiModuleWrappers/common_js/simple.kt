// EXPECTED_REACHABLE_NODES: 496
// MODULE: module1
// FILE: bar.kt
// MODULE_KIND: COMMON_JS
fun bar() = "bar"

// MODULE: main(module1)
// FILE: box.kt
// MODULE_KIND: COMMON_JS
fun box(): String {
    assertEquals("bar", bar())
    return "OK"
}