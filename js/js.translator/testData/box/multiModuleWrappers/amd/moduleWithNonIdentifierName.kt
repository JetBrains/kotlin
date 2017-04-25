// EXPECTED_REACHABLE_NODES: 495
// MODULE: module-1
// FILE: bar.kt
// MODULE_KIND: AMD
fun bar() = "bar"

// MODULE: main(module-1)
// FILE: box.kt
// MODULE_KIND: AMD
fun box(): String {
    assertEquals("bar", bar())
    return "OK"
}