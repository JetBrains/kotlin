// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1283
// MODULE: module-1
// FILE: bar.kt
// MODULE_KIND: PLAIN
fun bar() = "bar"

// MODULE: main(module-1)
// FILE: box.kt
// MODULE_KIND: PLAIN
fun box(): String {
    assertEquals("bar", bar())
    return "OK"
}