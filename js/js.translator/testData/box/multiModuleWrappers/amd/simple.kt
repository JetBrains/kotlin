// EXPECTED_REACHABLE_NODES: 1284
// MODULE: module1
// MODULE_KIND: AMD
// FILE: bar.kt
fun bar() = "bar"

// MODULE: main(module1)
// MODULE_KIND: AMD
// FILE: box.kt
fun box(): String {
    assertEquals("bar", bar())
    return "OK"
}
