// EXPECTED_REACHABLE_NODES: 1286
// MODULE: module1
// MODULE_KIND: UMD
// FILE: bar.kt
fun bar() = "bar"

// MODULE: main(module1)
// MODULE_KIND: UMD
// FILE: box.kt
fun box(): String {
    assertEquals("bar", bar())
    return "OK"
}
