// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_MINIFICATION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: member-properties.kt

package foo

@JsExport
class Test {
    val _val: Int = 1

    var _var: Int = 1

    val _valCustom: Int
        get() = 1

    val _valCustomWithField: Int = 1
        get() = field + 1

    var _varCustom: Int
        get() = 1
        set(value) {}

    var _varCustomWithField: Int = 1
        get() = field * 10
        set(value) { field = value * 10 }
}