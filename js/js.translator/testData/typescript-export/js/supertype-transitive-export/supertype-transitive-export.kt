// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// WITH_STDLIB

// FILE: foo.kt

// Test that even if List is never referenced in explicitly exported declarations, it is still exported by virtue of it being a supertype
// of MutableList, which _is_ referenced.
@JsExport
fun foo(ml: MutableList<Int>) {}

@JsExport.Ignore
class NonExportedSet(val set: MutableSet<String>): MutableSet<String> by set

// Test that even if MutableSet is never referenced in explicitly exported declarations, it is still exported by virtue of it being a supertype
// of NonExportedSet, which _is_ referenced, but not exported either.
@JsExport
val exportedProperty: NonExportedSet
    get() = NonExportedSet(mutableSetOf("O", "K"))
