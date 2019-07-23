// IGNORE_BACKEND: JS
// EXPECTED_REACHABLE_NODES: 1270
// SKIP_MINIFICATION
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

// MODULE: if
// FILE: lib.kt
@JsName("foo")
@JsExport
public fun foo(k: String): String = "O$k"

// FILE: test.js
function box() {
    return this["if"].foo("K");
}