// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// WITH_STDLIB

// Test that even if List is never referenced in explicitly exported declarations, it is still exported by virtue of it being a supertype
// of MutableList, which _is_ referenced.

// FILE: foo.kt
@JsExport
fun foo(ml: MutableList<Int>) {}
