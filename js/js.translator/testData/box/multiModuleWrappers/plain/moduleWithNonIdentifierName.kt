// MODULE: module_1
// JS_MODULE_KIND: PLAIN
// FILE: bar.kt
fun bar() = "bar"

// MODULE: main(module_1)
// JS_MODULE_KIND: PLAIN
// FILE: box.kt
fun box(): String {
    assertEquals("bar", bar())
    return "OK"
}
