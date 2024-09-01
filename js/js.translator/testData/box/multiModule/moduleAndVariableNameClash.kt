// KJS_WITH_FULL_RUNTIME
// MODULE: module1
// FILE: module1.kt

fun bar() = "bar"

// MODULE: main(module1)
// FILE: main.kt

fun box(): String {
    var module1 = bar()
    assertEquals("bar", module1)

    return "OK"
}
