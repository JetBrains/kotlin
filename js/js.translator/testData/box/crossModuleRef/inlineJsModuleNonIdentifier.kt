// EXPECTED_REACHABLE_NODES: 1316
// MODULE: lib_1
// FILE: lib_1.js
define("lib_1", [], function() {
    return function() {
        return "OK";
    }
})

// MODULE: lib2(lib_1)
// FILE: lib2.kt
// MODULE_KIND: AMD
@JsModule("lib_1")
external fun foo(): String

// MODULE: lib3(lib2)
// FILE: lib3.kt
// MODULE_KIND: AMD
inline fun bar() = foo()

// MODULE: main(lib3)
// FILE: main.kt
// MODULE_KIND: AMD

fun box() = bar()