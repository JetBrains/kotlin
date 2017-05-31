// EXPECTED_REACHABLE_NODES: 490
// MODULE: lib1
// FILE: lib1.js
define("lib1", [], function() {
    return {
        foo: function() {
            return "OK";
        }
    };
})

// MODULE: lib2(lib1)
// FILE: lib2.kt
// MODULE_KIND: AMD
@file:JsModule("lib1")

external fun foo(): String

// MODULE: lib3(lib2)
// FILE: lib3.kt
// MODULE_KIND: AMD
inline fun bar() = foo()

// MODULE: main(lib3)
// FILE: main.kt
// MODULE_KIND: AMD

fun box() = bar()