// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1283
// MODULE: main(module1)
// FILE: main.kt

fun box(): String {
    var module1 = bar()
    assertEquals("bar", module1)

    return "OK"
}

// MODULE: module1
// FILE: module1.kt

fun bar() = "bar"