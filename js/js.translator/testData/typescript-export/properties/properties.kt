// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_MINIFICATION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: properties.kt

package foo

@JsExport
const val _const_val: Int = 1

@JsExport
val _val: Int = 1

@JsExport
var _var: Int = 1

@JsExport
val _valCustom: Int
    get() = 1

@JsExport
val _valCustomWithField: Int = 1
    get() = field + 1

@JsExport
var _varCustom: Int
    get() = 1
    set(value) {}

@JsExport
var _varCustomWithField: Int = 1
    get() = field * 10
    set(value) { field = value * 10 }