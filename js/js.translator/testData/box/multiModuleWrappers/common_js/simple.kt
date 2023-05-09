// EXPECTED_REACHABLE_NODES: 1285
// MODULE: module1
// MODULE_KIND: COMMON_JS
// FILE: bar.kt
fun bar() = "bar"

// MODULE: main(module1)
// MODULE_KIND: COMMON_JS
// FILE: box.kt
fun box(): String {
    assertEquals("bar", bar())
    return "OK"
}
