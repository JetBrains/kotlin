// MODULE: module1
// JS_MODULE_KIND: COMMON_JS
// FILE: bar.kt
fun bar() = "bar"

// MODULE: main(module1)
// JS_MODULE_KIND: COMMON_JS
// FILE: box.kt
fun box(): String {
    assertEquals("bar", bar())
    return "OK"
}
