// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE
// SKIP_NODE_JS

// MODULE: non_identifier_module_name
// FILE: lib.kt
@JsName("foo")
@JsExport
public fun foo(k: String): String = "O$k"

// FILE: test.js
function box() {
    return this["non_identifier_module_name"].foo("K");
}
