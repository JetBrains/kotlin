// KJS_WITH_FULL_RUNTIME
// EXPECTED_REACHABLE_NODES: 1283
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
