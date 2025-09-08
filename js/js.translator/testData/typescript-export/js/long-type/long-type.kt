// IGNORE_BACKEND: JS_IR
// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: long-type.kt

package foo

@JsExport
val _long: Long = 1L

@JsExport
val _long_array: LongArray = longArrayOf()

@JsExport
val _array_long: Array<Long> = emptyArray()

// Nullable types
@JsExport
val _n_long: Long? = 1?.toLong()
