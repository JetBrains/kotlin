// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES
// MODULE: lib2
// FILE: lib1.mjs
export default function() {
    return "OK";
}

// FILE: lib2.kt
@JsModule("./lib1.mjs")
external fun foo(): String

// MODULE: lib3(lib2)
// FILE: lib3.kt
inline fun bar() = foo()

// MODULE: main(lib3)
// FILE: main.kt

fun box() = bar()
