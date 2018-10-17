// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1316
// MODULE: lib-1
// FILE: lib-1.js
define("lib-1", [], function() {
    return function() {
        return "OK";
    }
})

// MODULE: lib2(lib-1)
// FILE: lib2.kt
// MODULE_KIND: AMD
@JsModule("lib-1")
external fun foo(): String

// MODULE: lib3(lib2)
// FILE: lib3.kt
// MODULE_KIND: AMD
inline fun bar() = foo()

// MODULE: main(lib3)
// FILE: main.kt
// MODULE_KIND: AMD

fun box() = bar()