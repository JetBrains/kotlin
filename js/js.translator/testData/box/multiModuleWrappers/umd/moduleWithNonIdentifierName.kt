// EXPECTED_REACHABLE_NODES: 497
// MODULE: module-1
// FILE: bar.kt
// MODULE_KIND: UMD
fun bar() = "bar"

// MODULE: main(module-1)
// FILE: box.kt
// MODULE_KIND: UMD
fun box(): String {
    assertEquals("bar", bar())
    return "OK"
}