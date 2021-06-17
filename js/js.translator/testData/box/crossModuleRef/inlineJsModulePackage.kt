// DONT_TARGET_EXACT_BACKEND: JS
// SKIP_OLD_MODULE_SYSTEMS
// FILE: lib1.mjs
export function foo() {
    return "OK";
}

// MODULE: lib2
// FILE: lib2.kt
@file:JsModule("./lib1.mjs")

external fun foo(): String

// MODULE: lib3(lib2)
// FILE: lib3.kt
inline fun bar() = foo()

// MODULE: main(lib3)
// FILE: main.kt

fun box() = bar()