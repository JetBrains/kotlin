// MODULE: module_1
// JS_MODULE_KIND: AMD
// FILE: bar.kt
fun bar() = "bar"

// MODULE: main(module_1)
// JS_MODULE_KIND: AMD
// FILE: box.kt
fun box(): String {
    assertEquals("bar", bar())
    return "OK"
}
