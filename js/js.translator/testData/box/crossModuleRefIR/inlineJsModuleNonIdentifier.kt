// SPLIT_PER_MODULE
// EXPECTED_REACHABLE_NODES: 1316
// MODULE: lib_1
// FILE: lib_1.js
define("lib_1", [], function() {
    return function() {
        return "OK";
    }
})

// MODULE: lib2(lib_1)
// MODULE_KIND: AMD
// FILE: lib2.kt
@JsModule("lib_1")
external fun foo(): String

// MODULE: lib3(lib2)
// MODULE_KIND: AMD
// FILE: lib3.kt
inline fun bar() = foo()

// MODULE: main(lib3)
// MODULE_KIND: AMD
// FILE: main.kt

fun box() = bar()
