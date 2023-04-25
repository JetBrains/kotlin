// EXPECTED_REACHABLE_NODES: 1316
// MODULE: lib1
// FILE: lib1.js
define("lib1", [], function() {
    return function() {
        return "OK";
    }
})

// MODULE: lib2(lib1)
// MODULE_KIND: AMD
// FILE: lib2.kt
@JsModule("lib1")
external fun foo(): String

// MODULE: lib3(lib2)
// MODULE_KIND: AMD
// FILE: lib3.kt
inline fun bar() = foo()

// MODULE: main(lib3)
// MODULE_KIND: AMD
// FILE: main.kt

fun box() = bar()
