// EXPECTED_REACHABLE_NODES: 495
// MODULE: module1
// FILE: bar.kt
// MODULE_KIND: AMD
fun bar() = "bar"

// MODULE: main(module1)
// FILE: box.kt
// MODULE_KIND: AMD
fun box(): String {
    assertEquals("bar", bar())
    return "OK"
}