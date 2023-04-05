// EXPECTED_REACHABLE_NODES: 1283
// MODULE: module_1
// MODULE_KIND: PLAIN
// FILE: bar.kt
fun bar() = "bar"

// MODULE: main(module_1)
// MODULE_KIND: PLAIN
// FILE: box.kt
fun box(): String {
    assertEquals("bar", bar())
    return "OK"
}
