// EXPECTED_REACHABLE_NODES: 1118
// MODULE: module1
// FILE: bar.kt
// MODULE_KIND: UMD
fun bar() = "bar"

// MODULE: main(module1)
// FILE: box.kt
// MODULE_KIND: UMD
fun box(): String {
    assertEquals("bar", bar())
    return "OK"
}